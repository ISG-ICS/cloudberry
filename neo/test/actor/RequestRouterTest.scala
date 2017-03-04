package actor

import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.TestProbe
import akka.stream.scaladsl.Source
import akka.util.ByteString
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.actor.TestkitExample
import java.util.concurrent.Executors
import models.{GeoLevel, TimeBin, UserRequest}
import org.joda.time.{DateTimeZone, Interval}
import org.specs2.mutable.SpecificationLike
import org.specs2.mock.Mockito
import org.mockito.Mockito._
import play.api.libs.json._
import play.api.libs.ws._
import play.api.http.Writeable

import scala.concurrent.{ExecutionContext, Future}

class RequestRouterTest extends TestkitExample with SpecificationLike with Mockito{

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))
  implicit val materializer: Materializer = ActorMaterializer()

  "RequestRouter" should {
    "XXXX" in {

      ok
    }
  }
}