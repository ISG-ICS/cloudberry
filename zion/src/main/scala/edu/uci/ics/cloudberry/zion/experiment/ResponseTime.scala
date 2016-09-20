package edu.uci.ics.cloudberry.zion.experiment

import java.io.File
import java.util.concurrent.Executors

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import edu.uci.ics.cloudberry.zion.model.datastore.AsterixConn
import edu.uci.ics.cloudberry.zion.model.impl.{AQLGenerator, TwitterDataStore}
import edu.uci.ics.cloudberry.zion.model.schema._
import org.asynchttpclient.AsyncHttpClientConfig
import org.joda.time.{DateTime, Duration}
import play.api.libs.json.JsValue
import play.api.libs.ws.WSConfigParser
import play.api.libs.ws.ahc.{AhcConfigBuilder, AhcWSClient, AhcWSClientConfig}
import play.api.{Configuration, Environment, Mode}

import scala.concurrent.{Await, ExecutionContext}

object ResponseTime extends App {
  val gen = new AQLGenerator()

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  val wsClient = produceWSClient()
  val url = "http://actinium.ics.uci.edu:19002/aql"
  val asterixConn = new AsterixConn(url, wsClient)

  testRealTime()

  def exit(): Unit = {
    wsClient.close()
    System.exit(0)
  }

  def produceWSClient(): AhcWSClient = {
    val configuration = Configuration.reference ++ Configuration(ConfigFactory.parseString(
      """
        |play.ws.followRedirects = true
        |play {
        |  ws {
        |    timeout {
        |      connection = 60000
        |      idle = 600000
        |    }
        |  }
        |}
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

  def timeQuery(aql: String, field: String): (Long, Int) = {
    val start = DateTime.now()
    val f = asterixConn.postQuery(aql).map { ret =>
      val duration = new Duration(start, DateTime.now())
      val sum = (ret \\ field).map(_.as[Int]).sum
      //      println("time: " + duration.getMillis + " " + field + ": " + value)
      (duration.getMillis, sum)
    }
    Await.result(f, scala.concurrent.duration.Duration.Inf)
  }

  def warmUp(): Unit = {
    val aql =
      """
        |for $d in dataset twitter.ds_tweet
        |where $d.create_at >= datetime('2016-08-01T08:00:00.000Z') and
        |      $d.create_at <= datetime('2016-08-02T08:00:00.000Z')
        |group by $g := get-day($d.create_at) with $d
        |return { "day": $g, "count": count($d)}
      """.stripMargin
    val f = asterixConn.postQuery(aql)
    Await.result(f, scala.concurrent.duration.Duration.Inf)
  }

  def clearCache(): Unit = {
    val aql =
      """
        |for $d in dataset twitter.ds_tweet
        |where $d.create_at >= datetime('2016-08-01T08:00:00.000Z') and
        |      $d.create_at <= datetime('2016-08-15T08:00:00.000Z')
        |group by $g := get-day($d.create_at) with $d
        |return { "day": $g, "count": count($d)}
      """.stripMargin
    val f = asterixConn.postQuery(aql)
    Await.result(f, scala.concurrent.duration.Duration.Inf)
  }

  def testRealTime(): Unit = {
    clearCache()
    val gaps = Seq(1, 2, 4, 8, 16, 32)
    val times = 5
    val keywords = Seq("happy", "zika", "uci", "trump")

    for (gap <- gaps) {
      for (keyword <- keywords) {
        var sum = 0l
        var count = 0
        var firstTime = 0l
        0 to times foreach { i =>
          val now = DateTime.now()
          val aql = getAQL(now.minusHours(gap), gap, keyword)
          val (time, c) = timeQuery(aql, "count")
          if (i == 0) {
            firstTime = time
          } else {
            sum += time
            count = c
          }
        }
        println(s"gap:$gap\tkeyword:$keyword")
        println(s"firstTime\t$firstTime")
        println(s"avgTime\t${sum * 1.0 / times}")
        println(s"count\t$count")
      }
    }
  }

  def testHistory(): Unit = {
    //  val countPerGap = 90000 // global
    //  val countPerGap = 1000 // happy
    //  val countPerGap = 250 // trump
    val countPerGap = 250
    // a
    val gap = 80
    var times = 0

    warmUp()
    1 to 30 foreach { day =>
      var start = new DateTime(2016, 9, day, 8, 0)
      val stop = start.plusHours(24)

      while (start.getMillis < stop.getMillis) {
        val aql = getAQL(start, gap, "happy")
        val (time, count) = timeQuery(aql, "count")
        if (count > countPerGap * gap) {
          times += 1
          if (times > 10) {
            exit()
          }
        }
        start = start.plusHours(1)
      }
    }
  }

  def getAQL(start: DateTime, gapHour: Int, keyword: String): String = {
    val aggrCount = AggregateStatement("*", Count, "count")
    val keywordFilter = FilterStatement("text", None, Relation.contains, Seq(keyword))
    val timeFilter = FilterStatement("create_at", None, Relation.inRange,
                                     Seq(TimeField.TimeFormat.print(start),
                                         TimeField.TimeFormat.print(start.plusHours(gapHour))))

    val globalAggr = GlobalAggregateStatement(aggrCount)
    val byHour = ByStatement("create_at", Some(Interval(TimeUnit.Minute, 10 * gapHour)), Some("hour"))
    val groupStatement = GroupStatement(Seq(byHour), Seq(aggrCount))
    //      val query = Query(dataset = "twitter.ds_tweet", filter = Seq(timeFilter), groups = Some(groupStatement))
    val query = Query(dataset = "twitter.ds_tweet", filter = Seq(timeFilter, keywordFilter), globalAggr = Some(globalAggr))
    gen.generate(query, TwitterDataStore.TwitterSchema)
  }

  exit()
}
