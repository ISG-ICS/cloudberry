package controllers

import javax.inject.{Inject, Singleton}

import model.MySqlMigration_20170810
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.{Configuration, Environment, Logger}

import scala.concurrent.Await
import scala.concurrent.duration._

@Singleton
class MySqlTwitterMapApplication @Inject()(val wsClient: WSClient,
                                      val config: Configuration,
                                      val environment: Environment) extends Controller {

  val cloudberryRegisterURL: String = config.getString("cloudberry.register").getOrElse("http://localhost:9000/admin/register")
  val cloudberryWS: String = config.getString("cloudberry.ws").getOrElse("ws://localhost:9000/ws")
  val sentimentEnabled: Boolean = config.getBoolean("sentimentEnabled").getOrElse(false)
  val sentimentUDF: String = config.getString("sentimentUDF").getOrElse("twitter.`snlp#getSentimentScore`(text)")
  val removeSearchBar: Boolean = config.getBoolean("removeSearchBar").getOrElse(false)
  val predefinedKeywords: Seq[String] = config.getStringSeq("predefinedKeywords").getOrElse(Seq())
  val startDate: String = config.getString("startDate").getOrElse("2015-11-22T00:00:00.000")
  val endDate : Option[String] = config.getString("endDate")

  val clientLogger = Logger("client")

  val register = MySqlMigration_20170810.migration.up(wsClient, cloudberryRegisterURL)
  Await.result(register, 1 minutes)

  def mysqlTwittermap = Action { request =>
    val remoteAddress = request.remoteAddress
    val userAgent = request.headers.get("user-agent").getOrElse("unknown")
    clientLogger.info(s"Connected: user_IP_address = $remoteAddress; user_agent = $userAgent")
    Ok(views.html.twittermap.index("TwitterMap", cloudberryWS, startDate, endDate, sentimentEnabled, sentimentUDF, removeSearchBar, predefinedKeywords, false, true))
  }

}
