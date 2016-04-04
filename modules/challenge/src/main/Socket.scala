package lila.challenge

import akka.actor._
import play.api.libs.json._
import scala.concurrent.duration.Duration

import lila.hub.TimeBomb
import lila.socket.actorApi._
import lila.socket.{ SocketActor, History, Historical }

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

    case GetVersion                    => sender ! history.version

    case Socket.AddMember(uid, member) => addMember(uid, member)

    case Quit(uid)                     => quit(uid)
  }

  protected def shouldSkipMessageFor(message: Message, member: Socket.Member) = false
}

private object Socket {

  case class Member(
      out: ActorRef,
      userId: Option[String],
      owner: Boolean) extends lila.socket.SocketMember {
    val troll = false
  }

  case class AddMember(uid: String, member: Member)

  case object Reload
}
