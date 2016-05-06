package edu.uci.ics.cloudberry.zion.actor

import edu.uci.ics.cloudberry.zion.asterix.AsterixConnection
import play.api.Play
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import play.api.test.WsTestClient
import play.api.routing.sird._
import play.core.server.Server

import scala.concurrent.ExecutionContext

object TestUtil {
  /**
    * @param block
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
        block(new AsterixConnection(client, "/aql"))
      }
    }
  }

}

