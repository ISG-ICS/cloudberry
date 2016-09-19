package edu.uci.ics.cloudberry.zion.experiment

import java.io.File
import java.util.concurrent.Executors

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import edu.uci.ics.cloudberry.zion.model.datastore.AsterixConn
import edu.uci.ics.cloudberry.zion.model.impl.{AQLGenerator, TwitterDataStore}
import edu.uci.ics.cloudberry.zion.model.schema._
import io.netty.handler.logging.LogLevel
import org.asynchttpclient.AsyncHttpClientConfig
import org.joda.time.{DateTime, Duration}
import play.api.libs.ws.WSConfigParser
import play.api.{Configuration, Environment, Logger, Mode}
import play.api.libs.ws.ahc.{AhcConfigBuilder, AhcWSClient, AhcWSClientConfig}

import scala.concurrent.{Await, ExecutionContext, Future}

object ResponseTime extends App {
  val gen = new AQLGenerator()

//  Logger.apply("io.netty").underlyingLogger.

  println("get it: " + Logger.apply("io.netty").isDebugEnabled)

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  val wsClient = produceWSClient()
  val url = "http://actinium.ics.uci.edu:19002/aql"
  val asterixConn = new AsterixConn(url, wsClient)

  val aggrCount = AggregateStatement("*", Count, "count")
  var start = new DateTime(2016, 9, 1, 8, 0)
  val stop = start.plusHours(12)
  val gap = 1
  val keywordFilter = FilterStatement("text", None, Relation.contains, Seq("zika"))

  while (start.getMillis < stop.getMillis) {
    val timeFilter = FilterStatement("create_at", None, Relation.inRange,
                                     Seq(TimeField.TimeFormat.print(start),
                                         TimeField.TimeFormat.print(start.plusHours(gap))))

    val globalAggr = GlobalAggregateStatement(aggrCount)
    val query = Query(dataset = "twitter.ds_tweet", filter = Seq(timeFilter), globalAggr = Some(globalAggr))
    val aql = gen.generate(query, TwitterDataStore.TwitterSchema)

    timeQuery(aql)
    start = start.plusHours(gap)
  }
  wsClient.close()
  System.exit(0)


  def produceWSClient(): AhcWSClient = {
    val configuration = Configuration.reference ++ Configuration(ConfigFactory.parseString(
      """
        |play.ws.followRedirects = true
      """.stripMargin))

    // If running in Play, environment should be injected
    val environment = Environment(new File("."), this.getClass.getClassLoader, Mode.Prod)

    val parser = new WSConfigParser(configuration, environment)
    val config = new AhcWSClientConfig(wsClientConfig = parser.parse())
    val builder = new AhcConfigBuilder(config)
    val logging = new AsyncHttpClientConfig.AdditionalChannelInitializer() {
      override def initChannel(channel: io.netty.channel.Channel): Unit = {
        //        channel.pipeline.addFirst("log", new io.netty.handler.logging.LoggingHandler("info", LogLevel.ERROR))
      }
    }
    val ahcBuilder = builder.configure()
    ahcBuilder.setHttpAdditionalChannelInitializer(logging)
    val ahcConfig = ahcBuilder.build()
    new AhcWSClient(ahcConfig)
  }

  def timeQuery(aql: String): Unit = {
    val start = DateTime.now()
    val f = asterixConn.postQuery(aql).map { ret =>
      val duration = new Duration(start, DateTime.now())
      println("time: " + duration.getMillis)
      println(ret)
    }
    Await.result(f, scala.concurrent.duration.Duration.Inf)
  }
}
