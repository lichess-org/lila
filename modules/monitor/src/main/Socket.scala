package lila.monitor

import actorApi._
import lila.socket._
import lila.socket.actorApi.Connected

import akka.actor._
import play.api.libs.json._
import play.api.libs.iteratee._
import scala.concurrent.duration.Duration

private[monitor] final class Socket(timeout: Duration) extends SocketActor[Member](timeout) {

  def receiveSpecific = {

    case Join(uid) ⇒ {
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel)
      addMember(uid, member)
      sender ! Connected(enumerator, member)
    }

    case MonitorData(data) ⇒ notifyAll("monitor", data mkString ";")
  }
}
