package lila.monitor

import scala.concurrent.duration.Duration

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._

import actorApi._
import lila.socket._
import lila.socket.actorApi.{ PopulationGet, Connected, Broom }

private[monitor] final class Socket(timeout: Duration) extends SocketActor[Member](timeout) {

  def receiveSpecific = {

    // don't eject members - they don't ping the monitor socket
    case Broom         =>

    case PopulationGet => sender ! members.size

    case Join(uid) => {
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel)
      addMember(uid, member)
      sender ! Connected(enumerator, member)
    }

    case MonitorData(data) => notifyAll("monitor", data mkString ";")
  }
}
