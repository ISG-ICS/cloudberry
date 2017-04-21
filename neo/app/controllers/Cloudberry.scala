package controllers

import javax.inject.{Inject, Singleton}

import actor.RequestRouter
import akka.actor._
import akka.pattern.ask
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.util.Timeout
import controllers.Cloudberry.DataSetInfoView
import db.Migration_20160814
import edu.uci.ics.cloudberry.zion.actor.DataStoreManager.{DataManagerResponse, Register, _}
import edu.uci.ics.cloudberry.zion.actor.{BerryClient, DataStoreManager}
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.impl._
import play.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsSuccess, JsValue, _}
import play.api.libs.streams.ActorFlow
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller, WebSocket}
import play.api.{Configuration, Environment}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

@Singleton
class Cloudberry @Inject()(val wsClient: WSClient,
                           val configuration: Configuration,
                           val environment: Environment)
                          (implicit val system: ActorSystem,
                           implicit val materializer: Materializer
                          ) extends Controller {
  val config = new Config(configuration)
  val (asterixConn, qlGenerator) =
    config.AsterixLang match {
      case "aql" => (new AsterixAQLConn(config.AsterixURL, wsClient), AQLGenerator)
      case "sqlpp" => (new AsterixSQLPPConn(config.AsterixURL, wsClient), SQLPPGenerator)
      case _ => throw new IllegalArgumentException(s"unknown asterixdb.lang option:${config.AsterixLang}")
    }

  Await.result(Migration_20160814.migration.up(asterixConn), 10.seconds)

  val manager = system.actorOf(DataStoreManager.props(Migration_20160814.berryMeta, asterixConn, qlGenerator, config))

  Logger.info("I'm initializing")

  val listener = system.actorOf(Props(classOf[Listener], this))
  system.eventStream.subscribe(listener, classOf[DeadLetter])

  def index = Action.async {
    implicit val askTimeOut: Timeout = config.UserTimeOut
    (manager ? DataStoreManager.ListAllDataset).map { case dataset : Seq[DataSetInfo] =>
      Ok(views.html.cloudberry(dataset.map(DataSetInfo.write)))
    }
  }

  def dashboard = Action {
    Ok(views.html.dashboard("Dashboard"))
  }

  def ws = WebSocket.accept[JsValue, JsValue] { request =>
    ActorFlow.actorRef { out =>
      RequestRouter.props(BerryClient.props(new JSONParser(), manager, new QueryPlanner(), config, out), config)
    }
  }

  class Listener extends Actor with ActorLogging {
    def receive = {
      case d: DeadLetter => Logger.info(d.toString)
    }
  }

  def berryQuery = Action(parse.json) { request =>
    implicit val timeout: Timeout = Timeout(config.UserTimeOut)
    val source = Source.single(request.body)

    val flow = Cloudberry.actorFlow[JsValue, JsValue]({ out =>
      BerryClient.props(new JSONParser(), manager, new QueryPlanner(), config, out)
    }, BerryClient.Done)
    // ??? do we need to convert it to string ??? will be more clear after we have the use case.
    val toStringFlow = Flow[JsValue].map(js => js.toString() + System.lineSeparator())
    Ok.chunked((source via flow) via toStringFlow)
  }

  def register = Action.async(parse.json) { request =>
    implicit val timeout: Timeout = Timeout(config.UserTimeOut)

    request.body.validate[Register] match {
      case registerRequest: JsSuccess[Register] =>
        val newTable = registerRequest.get
        val receipt = (manager ? newTable).mapTo[DataManagerResponse]

        receipt.map { r =>
          if (r.isSuccess) Ok(r.message)
          else BadRequest(r.message)
        }.recover { case e =>
          InternalServerError("Fail to get receipt from dataStore manager. " + e.toString)
        }

      case e: JsError =>
        Future {
          BadRequest("Not a valid register Json POST: " + e.toString)
        }
    }
  }

  def deregister = Action.async(parse.json) { request =>
    implicit val timeout: Timeout = Timeout(config.UserTimeOut)

    request.body.validate[Deregister] match {
      case deregisterRequest: JsSuccess[Deregister] =>
        val dropTable = deregisterRequest.get
        val receipt = (manager ? dropTable).mapTo[DataManagerResponse]

        receipt.map { r =>
          if (r.isSuccess) Ok(r.message)
          else BadRequest(r.message)
        }.recover { case e =>
          InternalServerError("Fail to get receipt from dataStore manager. " + e.toString)
        }

      case e: JsError =>
        Future {
          BadRequest("Not valid Json POST: " + e.toString)
        }
    }
  }

}

object Cloudberry {
  case class DataSetInfoView(name: String, info: String)

  val createDatasetForm = Form(
    mapping(
      "name" -> text,
      "info" -> text
    )(DataSetInfoView.apply)(DataSetInfoView.unapply))

  /**
    * Copy and revised based on [[play.api.libs.streams.ActorFlow.actorRef()]]
    * The difference is that the '''flowActor''' will not kill the proped actor when it receives the [[Status.Success]]
    * ,because our actor still need to generate multiple responses for one request.
    * To finish the stream, the working actor (generated by the `props` function) needs to provide the `onCompleteMessage`
    * to let the `deleteActor` know that the processing has finished.
    * Then it will finish the stream by sending the [[Status.Success]] to the downstream actor.
    */
  def actorFlow[In, Out](props: ActorRef => Props, onCompleteMessage: Any, bufferSize: Int = 16, overflowStrategy: OverflowStrategy = OverflowStrategy.dropNew)
                        (implicit factory: ActorRefFactory, mat: Materializer): Flow[In, Out, _] = {

    // The stream can be completed successfully by sending [[akka.actor.PoisonPill]]
    // or [[akka.actor.Status.Success]] to the outActor.
    val (outActor, publisher) = Source.actorRef[Out](bufferSize, overflowStrategy)
      .toMat(Sink.asPublisher(false))(Keep.both).run()

    Flow.fromSinkAndSource(
      Sink.actorRef(factory.actorOf(Props(new Actor {

        /**
          * Delegate actor to wrap the `onCompleteMessage` with [[Status.Success]] in order to finish the stream
          */
        val delegateActor: ActorRef = context.watch(context.actorOf(Props(new Actor {
          override def receive: Receive = {
            case `onCompleteMessage` => outActor ! Status.Success(())
            case Terminated(_) =>
              Logger.info("Child terminated, stopping")
              context.stop(self)
            case other => outActor ! other
          }
        }), "delegateActor"))

        val flowActor: ActorRef = context.watch(context.actorOf(props(delegateActor), "flowActor"))
        context.watch(outActor)

        def receive: Receive = {
          case Status.Failure(error) =>
            Logger.error("flowActor receive status.failure" + error)
            flowActor ! PoisonPill
            delegateActor ! PoisonPill
          case Terminated(_) =>
            Logger.info("Child terminated, stopping")
            context.stop(self)
          case other => flowActor ! other
        }

        override def supervisorStrategy = OneForOneStrategy() {
          case _ =>
            Logger.error("Stopping actor due to exception")
            SupervisorStrategy.Stop
        }
      })), akka.actor.Status.Success(())),
      Source.fromPublisher(publisher)
    )
  }
}
