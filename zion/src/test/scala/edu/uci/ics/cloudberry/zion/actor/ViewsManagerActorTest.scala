package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{ActorRef, Props}
import akka.testkit.TestProbe
import edu.uci.ics.cloudberry.zion.asterix.TestData
import edu.uci.ics.cloudberry.zion.model.{DBQuery, KeywordPredicate}
import org.specs2.mutable.SpecificationLike

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class ViewsManagerActorTest extends TestkitExample with SpecificationLike with MockConnClient with TestData {

  val sourceName = "src"
  val newkey = "key"
  val existedKey = ""
  var flushed = 0
  val sourceProbe = new TestProbe(system)
  val existedViewProbe = new TestProbe(system)
  val newViewProbe = new TestProbe(system)
  val newViewRecord = ViewMetaRecord(sourceName, newkey, StateDaySummary, startTime, lastVisitTime, lastUpdateTime, visitTimes, updateCycle)
  val initViewRecord = ViewMetaRecord(sourceName, existedKey, StateDaySummary, startTime, lastVisitTime, lastUpdateTime, visitTimes, updateCycle)

  class FakeManager(override val sourceName: String, override val sourceActor: ActorRef)(implicit ec: ExecutionContext)
    extends ViewsManagerActor(sourceName, sourceActor) {

    override def getViewKey(query: DBQuery): String = if (query.predicates.exists(_.isInstanceOf[KeywordPredicate])) newkey else existedKey

    override def flushMeta(): Unit = flushed += 1

    override def flushInterval: FiniteDuration = 1 hours

    override def createViewActor(key: String, query: DBQuery, fView: Future[ViewMetaRecord]): ActorRef = {
      if (key == initViewRecord.viewKey) existedViewProbe.ref else newViewProbe.ref
    }

    override def createViewStore(query: DBQuery): Future[ViewMetaRecord] = Future(newViewRecord)

    override def loadMetaStore: Future[Seq[ViewMetaRecord]] = Future(Seq(initViewRecord))

    def testReceive: Receive = {
      case FakeManager.getMeta => sender ! viewMeta.values.toSeq.sortBy(_.viewKey)
    }

    override protected def routine: Receive = super.routine orElse testReceive
  }

  object FakeManager {

    object getMeta

  }

  sequential

  "A ViewManager" should {
    "create a store if it is a new view " in {
      val sender = new TestProbe(system)
      val viewActor = system.actorOf(Props(new FakeManager(sourceName, sourceProbe.ref)(ec)))
      sender.send(viewActor, FakeManager.getMeta)
      val actualMsg = sender.receiveOne(500 millis)
      actualMsg must_== Seq(initViewRecord)
    }
    "use the existed viewActor to answer the query" in {
      val viewActor = system.actorOf(Props(new FakeManager(sourceName, sourceProbe.ref)(ec)))
      viewActor ! byStateByDayQuery
      existedViewProbe.expectMsg(byStateByDayQuery)
      newViewProbe.expectNoMsg(500 millis)
      ok
    }
    "create a new view store to answer the new query" in {
      val viewActor = system.actorOf(Props(new FakeManager(sourceName, sourceProbe.ref)(ec)))
      viewActor ! keywordQuery
      existedViewProbe.expectNoMsg()
      newViewProbe.expectMsg(keywordQuery)
      ok
    }
    "update the meta store when receive the update request" in {
      val viewActor = system.actorOf(Props(new FakeManager(sourceName, sourceProbe.ref)(ec)))
      viewActor ! keywordQuery
      existedViewProbe.expectNoMsg()
      newViewProbe.expectMsg(keywordQuery)

      val sender = new TestProbe(system)
      sender.send(viewActor, FakeManager.getMeta)
      val actualMsg = sender.receiveOne(500 millis)
      actualMsg must_== Seq(initViewRecord, newViewRecord).sortBy(_.viewKey)
    }
    "update the meta store when receives the request" in {
      ok
    }
  }
}
