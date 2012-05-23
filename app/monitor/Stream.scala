package lila
package monitor

import play.api.libs.iteratee._
import play.api.libs.concurrent.Promise
import akka.actor._
import akka.dispatch.Await
import akka.pattern.ask
import akka.util.duration._
import akka.util.Timeout

final class Stream(
    reporting: ActorRef,
    timeout: Int) {

  implicit val maxWait = 100 millis
  implicit val maxWaitTimeout = Timeout(maxWait)

  val getData = Enumerator.generateM {
    Promise.timeout(Some(data mkString ";"), timeout)
  }

  def maxMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024)

  private def data = Await.result(
    reporting ? GetMonitorData mapTo manifest[List[String]],
    maxWait
  ) 
}
