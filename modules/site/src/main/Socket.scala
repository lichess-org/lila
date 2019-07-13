package lila.site

import scala.concurrent.duration.Duration
import scala.concurrent.duration._

import play.api.libs.iteratee._
import play.api.libs.json.JsValue

import actorApi._
import lila.socket._
import lila.socket.actorApi.SendToFlag

private[site] final class Socket(
    system: akka.actor.ActorSystem,
    sriTtl: Duration
) extends SocketTrouper[Member](system, sriTtl) with LoneSocket {

  def monitoringName = "site"
  def broomFrequency = 4159 millis

  system.lilaBus.subscribe(this, 'sendToFlag)

  private val flags = new lila.socket.MemberGroup[Member](_.flag)

  def receiveSpecific = {

    case Join(sri, userId, flag, promise) =>
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, userId, flag)
      addMember(sri, member)
      flags.add(sri, member)
      promise success Connected(enumerator, member)

    case SendToFlag(flag, msg) =>
      flags get flag foreach {
        _.foreachValue(_ push msg)
      }
  }

  // don't eject non-pinging API socket clients
  override def broom: Unit = {
    members foreach {
      case (sri, member) => if (!aliveSris.get(sri) && !member.isApi) ejectSriString(sri)
    }
    lila.mon.socket.count.site(members.size)
  }

  override protected def afterQuit(sri: Socket.Sri, member: Member) = {
    flags.remove(sri, member)
  }
}
