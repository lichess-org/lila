package lila.site

import scala.concurrent.duration.Duration

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json.JsValue

import actorApi._
import lila.socket._
import lila.socket.actorApi.SendToFlag

private[site] final class Socket(timeout: Duration) extends SocketActor[Member](timeout) {

  override val startsOnApplicationBoot = true

  type UID = String
  type Flag = String

  val flags = new lila.socket.MemberGroup[Member](_.flag)

  def receiveSpecific = {

    case Join(uid, userId, flag) => {
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, userId, flag)
      addMember(uid.value, member)
      flags.add(uid.value, member)
      sender ! Connected(enumerator, member)
    }

    case SendToFlag(flag, msg) =>
      flags get flag foreach {
        _.foreachValue(_ push msg)
      }
  }

  // don't eject non-pinging API socket clients
  override def broom: Unit = {
    members foreach {
      case (uid, member) => if (!aliveUids.get(uid) && !member.isApi) eject(uid)
    }
  }

  override def quit(uid: String): Unit = {
    members get uid foreach { flags.remove(uid, _) }
    super.quit(uid)
  }
}
