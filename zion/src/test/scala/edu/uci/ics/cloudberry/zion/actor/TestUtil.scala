package edu.uci.ics.cloudberry.zion.actor

import edu.uci.ics.cloudberry.zion.asterix.AsterixConnection
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.KeyCountPair
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.specs2.mock.Mockito
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.mvc.{Action, _}
import play.api.routing.sird._
import play.api.test.WsTestClient
import play.api.{Configuration, Play}
import play.core.server.Server

import scala.concurrent.{ExecutionContext, Future}

object TestUtil extends Mockito {
  val mockPlayConfig = mock[Configuration]
  mockPlayConfig.getString(anyString, any) returns None
  val cloudberryConfig = new Config(mockPlayConfig)

  /** A heavy but complete way to test the ws suggested by official doc.
    * Too heavy for unit test, may be more approprate for intergration test.
    *
    * @param expectedResponse
    * @param block
    * @param ec
    * @tparam T
    * @return
    */
  def withAsterixConn[T](expectedResponse: JsValue)(block: AsterixConnection => T)(implicit ec: ExecutionContext): T = {
    Server.withRouter() {
      //* this mock server can't write the request, we can not let the result based on the request
      case POST(p"/aql") => Action {
        Results.Ok(expectedResponse)
      }
    } { implicit port =>
      implicit val materializer = Play.current.materializer
      WsTestClient.withClient { client =>
        block(new AsterixConnection("/aql", client, cloudberryConfig))
      }
    }
  }

}

trait MockConnClientOld extends Mockito {
  val mockPlayConfig = mock[Configuration]
  mockPlayConfig.getString(anyString, any) returns None
  val cloudberryConfig = new Config(mockPlayConfig)

  def withLightWeightConn[T](expectedResponse: JsValue)(block: AsterixConnection => T)(implicit ec: ExecutionContext): T = {
    val mockConn = mock[AsterixConnection]
    val mockResponse = mock[WSResponse]
    mockResponse.status returns (200)
    mockResponse.json returns (expectedResponse)
    when(mockConn.post(any[String])).thenAnswer(new Answer[Future[WSResponse]] {
      override def answer(invocation: InvocationOnMock): Future[WSResponse] = {
        println(invocation.getArguments.head.asInstanceOf[String])
        Future(mockResponse)
      }
    })
    block(mockConn)
  }

  def withAsterixConn[T](multipleResults: JsArray)(block: AsterixConnection => T)(implicit ec: ExecutionContext): T = {
    val mockConn = mock[AsterixConnection]
    val mockResponse = mock[WSResponse]
    mockResponse.status returns (200)
    mockResponse.body returns (multipleResults.value.map("[ " + _.toString() + "\n ]").mkString("\n"))
    when(mockConn.post(any[String])).thenAnswer(new Answer[Future[WSResponse]] {
      override def answer(invocation: InvocationOnMock): Future[WSResponse] = {
        println(invocation.getArguments.head.asInstanceOf[String])
        Future(mockResponse)
      }
    })
    block(mockConn)
  }

  def withAqlCheckConn[T](expectedAQL: String)(block: AsterixConnection => T)(implicit ec: ExecutionContext): T = {
    val mockConn = mock[AsterixConnection]
    val mockResponse = mock[WSResponse]
    mockResponse.status returns (200)
    val multipleResults = JsArray(Seq(Seq.empty[KeyCountPair], Seq.empty[KeyCountPair], Seq.empty[KeyCountPair]).map(Json.toJson(_)))
    mockResponse.body returns (multipleResults.value.map("[ " + _.toString() + "\n ]").mkString("\n"))
    when(mockConn.post(any[String])).thenAnswer(new Answer[Future[WSResponse]] {
      override def answer(invocation: InvocationOnMock): Future[WSResponse] = {
        //For debug purpose
        expectedAQL.trim === invocation.getArguments.head.asInstanceOf[String].trim
        if (expectedAQL.trim == invocation.getArguments.head.asInstanceOf[String].trim) {
          Future(mockResponse)
        } else {
          Future(null)
        }
      }
    })
    block(mockConn)
  }

  /**
    * Mock the aql and the reponse based on the given map
    *
    * @param aql2jsonAnswer the aql to Json response map
    * @param block
    * @param ec
    * @tparam T
    * @return
    */
  def withQueryAQLConn[T](aql2jsonAnswer: Map[String, JsValue])(block: AsterixConnection => T)(implicit ec: ExecutionContext): T = {
    val mockConn = mock[AsterixConnection]
    when(mockConn.postQuery(any[String], any)).thenAnswer(new Answer[Future[JsValue]] {
      override def answer(invocation: InvocationOnMock): Future[JsValue] = {
        val aql = invocation.getArguments.head.asInstanceOf[String].trim
        val optResponse = aql2jsonAnswer.get(aql)
        if (optResponse.isDefined){
          Future(optResponse.get)
        } else {
          System.err.println(aql)
          throw new IllegalArgumentException(aql)
        }
      }
    })
    block(mockConn)
  }

  /**
    * Mock the response without the AQL checking.
    *
    * @param jsAnswers sequence of Json response
    * @param block
    * @param ec
    * @tparam T
    * @return
    */
  def withQueryAQLConn[T](jsAnswers: Seq[JsValue])(block: AsterixConnection => T)(implicit ec: ExecutionContext): T = {
    val mockConn = mock[AsterixConnection]
    val iter = jsAnswers.iterator
    when(mockConn.postQuery(any[String], any)).thenAnswer(new Answer[Future[JsValue]] {
      override def answer(invocation: InvocationOnMock): Future[JsValue] = {
        val aql = invocation.getArguments.head.asInstanceOf[String].trim
        if (iter.hasNext) {
          Future(iter.next())
        } else {
          throw new IllegalArgumentException(aql)
        }
      }
    })
    block(mockConn)
  }

  def withUpdateAQLConn[T](aqlSet: Set[String])(block: AsterixConnection => T)(implicit ec: ExecutionContext): T = {
    val mockConn = mock[AsterixConnection]
    when(mockConn.postUpdate(any[String])).thenAnswer(new Answer[Future[Boolean]] {
      override def answer(invocation: InvocationOnMock): Future[Boolean] = {
        val aql = invocation.getArguments.head.asInstanceOf[String].trim
        if (aqlSet.apply(aql.trim)) {
          Future(true)
        } else {
          System.err.println(aql)
          Future(false)
        }
      }
    })
    block(mockConn)
  }

  def withSucceedUpdateAQLConn[T](block: AsterixConnection => T)(implicit ec: ExecutionContext): T = {
    val mockConn = mock[AsterixConnection]
    when(mockConn.postUpdate(any[String])).thenAnswer(new Answer[Future[Boolean]] {
      override def answer(invocation: InvocationOnMock): Future[Boolean] = {
        val aql = invocation.getArguments.head.asInstanceOf[String].trim
        println(aql)
        Future(true)
      }
    })
    block(mockConn)
  }

}
