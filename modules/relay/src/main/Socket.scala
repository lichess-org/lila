package lila.relay

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.concurrent.duration._

import actorApi._
import lila.hub.TimeBomb
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.{ SocketActor, History, Historical }
import lila.common.LightUser

private[relay] final class Socket(
    relayId: String,
    val history: History[Messadata],
    getRelay: () => Fu[Option[Relay]],
    jsonView: JsonView,
    lightUser: String => Option[LightUser],
    uidTimeout: Duration,
    socketTimeout: Duration) extends SocketActor[Member](uidTimeout) with Historical[Member, Messadata] {

  private val timeBomb = new TimeBomb(socketTimeout)

  private var delayedCrowdNotification = false

  def receiveSpecific = {

    case Reload => withRelay { relay =>
      jsonView(relay, none) foreach { obj =>
        notifyVersion("reload", obj, Messadata())
      }
    }

    case PingVersion(uid, v) => {
      ping(uid)
      timeBomb.delay
      withMember(uid) { m =>
        history.since(v).fold(resync(m))(_ foreach sendMessage(m))
      }
    }

    case Broom => {
      broom
      if (timeBomb.boom) self ! PoisonPill
    }

    case lila.chat.actorApi.ChatLine(_, line) => line match {
      case line: lila.chat.UserLine =>
        notifyVersion("message", lila.chat.Line toJson line, Messadata(line.troll))
      case _ =>
    }

    case GetVersion => sender ! history.version

    case Join(uid, user, version) =>
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user)
      addMember(uid, member)
      notifyCrowd
      sender ! Connected(enumerator, member)

    case Quit(uid) =>
      quit(uid)
      notifyCrowd

    case NotifyCrowd =>
      delayedCrowdNotification = false
      val (anons, users) = members.values.map(_.userId flatMap lightUser).foldLeft(0 -> List[LightUser]()) {
        case ((anons, users), Some(user)) => anons -> (user :: users)
        case ((anons, users), None)       => (anons + 1) -> users
      }
      notifyVersion("crowd", showSpectators(users, anons), Messadata())
  }

  def withRelay(f: Relay => Unit): Unit = getRelay() foreach {
    case None        => sys error s"No such relay $relayId"
    case Some(relay) => f(relay)
  }

  def notifyCrowd {
    if (!delayedCrowdNotification) {
      delayedCrowdNotification = true
      context.system.scheduler.scheduleOnce(500 millis, self, NotifyCrowd)
    }
  }

  protected def shouldSkipMessageFor(message: Message, member: Member) =
    message.metadata.trollish && !member.troll
}
