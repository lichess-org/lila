package lila.challenge

import akka.actor._
import play.api.libs.iteratee.Concurrent
import play.api.libs.json._
import scala.concurrent.duration.Duration

import lila.hub.TimeBomb
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.{ SocketActor, History, Historical }
import lila.socket.Socket.Uid

private final class Socket(
    challengeId: String,
    val history: History[Unit],
    getChallenge: Challenge.ID => Fu[Option[Challenge]],
    uidTimeout: Duration,
    socketTimeout: Duration) extends SocketActor[Socket.Member](uidTimeout) with Historical[Socket.Member, Unit] {

  private val timeBomb = new TimeBomb(socketTimeout)

  def receiveSpecific = {

    case Socket.Reload =>
      getChallenge(challengeId) foreach {
        _ foreach { challenge =>
          notifyVersion("reload", JsNull, ())
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

    case GetVersion => sender ! history.version

    case Socket.Join(uid, userId, owner) =>
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Socket.Member(channel, userId, owner)
      addMember(uid.value, member)
      sender ! Socket.Connected(enumerator, member)

    case Quit(uid) => quit(uid)
  }

  protected def shouldSkipMessageFor(message: Message, member: Socket.Member) = false
}

private object Socket {

  case class Member(
      channel: JsChannel,
      userId: Option[String],
      owner: Boolean) extends lila.socket.SocketMember {
    val troll = false
  }

  case class Join(uid: Uid, userId: Option[String], owner: Boolean)
  case class Connected(enumerator: JsEnumerator, member: Member)

  case object Reload
}
