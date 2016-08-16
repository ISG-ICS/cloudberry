package edu.uci.ics.cloudberry.zion.model.util

import edu.uci.ics.cloudberry.zion.model.datastore.IDataConn
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.specs2.mock.Mockito
import play.api.libs.json.JsValue

import scala.concurrent.{ExecutionContext, Future}

trait MockConnClient extends Mockito {

  import org.mockito.Mockito._
  /**
    * Mock the aql and the response based
    *
    * @return
    */
  def withQueryAQLConn[T](result: JsValue)(block: IDataConn => T)(implicit ec: ExecutionContext): T = {
    val mockConn = mock[IDataConn]
    when(mockConn.postQuery(any[String])).thenAnswer(new Answer[Future[JsValue]] {
      override def answer(invocation: InvocationOnMock): Future[JsValue] = {
        val aql = invocation.getArguments.head.asInstanceOf[String].trim
        Future(result)
      }
    })
    block(mockConn)
  }

}
