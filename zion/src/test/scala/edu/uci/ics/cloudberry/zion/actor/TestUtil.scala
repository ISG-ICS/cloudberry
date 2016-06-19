package edu.uci.ics.cloudberry.zion.actor

import edu.uci.ics.cloudberry.zion.asterix.AsterixConnection
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.KeyCountPair
import org.mockito.AdditionalAnswers
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

trait MockConnClient extends Mockito {
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

  def withQueryAQLConn[T](aql2jsonAnswer: Map[String, JsArray])(block: AsterixConnection => T)(implicit ec: ExecutionContext): T = {
    val mockConn = mock[AsterixConnection]
    aql2jsonAnswer.foreach { case (aql: String, json: JsArray) =>
      when(mockConn.postQuery(aql)).thenReturn(Future(json))
    }
    block(mockConn)
  }

  /**
    * Mock the response without the AQL checking.
    * @param jsAnswers
    * @param block
    * @param ec
    * @tparam T
    * @return
    */
  def withQueryAQLConn[T](jsAnswers: Seq[JsArray])(block: AsterixConnection => T)(implicit ec: ExecutionContext): T = {
    val mockConn = mock[AsterixConnection]
    import collection.JavaConverters._
    when(mockConn.postQuery(any[String])).thenAnswer(AdditionalAnswers.returnsElementsOf(jsAnswers.asJava))
    block(mockConn)
  }
}
