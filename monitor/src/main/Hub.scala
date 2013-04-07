package lila.monitor

import lila.socket._

import akka.actor._
import play.api.libs.json._
import play.api.libs.iteratee._
import scala.concurrent.duration.Duration

private[monitor] final class Hub(timeout: Duration) extends HubActor[Member](timeout) {

  def receiveSpecific = {

    case Join(uid) ⇒ {
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      addMember(uid, Member(channel))
      sender ! Connected(enumerator, channel)
    }

    case MonitorData(data) ⇒ notifyAll("monitor", data mkString ";")
  }
}
