package lila.site

import scala.concurrent.duration.Duration

import play.api.libs.iteratee._
import play.api.libs.json.JsValue

import actorApi._
import lila.socket._
import lila.socket.actorApi.SendToFlag

private[site] final class Socket(
    val system: akka.actor.ActorSystem,
    uidTtl: Duration
) extends SocketTrouper[Member](uidTtl) {

  private val flags = new lila.socket.MemberGroup[Member](_.flag)

  def receiveSpecific = {

    case JoinP(uid, userId, flag, promise) =>
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, userId, flag)
      addMember(uid, member)
      flags.add(uid, member)
      promise success Connected(enumerator, member)

    case SendToFlag(flag, msg) =>
      flags get flag foreach {
        _.foreachValue(_ push msg)
      }
  }

  // don't eject non-pinging API socket clients
  override def broom: Unit = {
    members foreach {
      case (uid, member) => if (!aliveUids.get(uid) && !member.isApi) ejectUidString(uid)
    }
    lila.mon.socket.count.site(members.size)
  }

  override def quit(uid: Socket.Uid): Unit = {
    members get uid.value foreach { flags.remove(uid, _) }
    super.quit(uid)
  }
}
