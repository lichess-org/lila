package lila.challenge

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.concurrent.duration.Duration
import scala.concurrent.Promise

import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.SocketTrouper
import lila.socket.Socket.{ Uid, GetVersionP, SocketVersion }
import lila.socket.{ History, Historical }

private final class ChallengeSocket(
    val system: ActorSystem,
    challengeId: String,
    val history: History[Unit],
    getChallenge: Challenge.ID => Fu[Option[Challenge]],
    uidTtl: Duration,
    keepMeAlive: () => Unit
) extends SocketTrouper[ChallengeSocket.Member](uidTtl) with Historical[ChallengeSocket.Member, Unit] {

  def receiveSpecific = {

    case ChallengeSocket.Reload =>
      getChallenge(challengeId) foreach {
        _ foreach { challenge =>
          notifyVersion("reload", JsNull, ())
        }
      }

    case Ping(uid, vOpt, lagCentis) =>
      ping(uid, lagCentis)
      pushEventsSinceForMobileBC(vOpt, uid)

    case GetVersionP(promise) => promise success history.version

    case ChallengeSocket.JoinP(uid, userId, owner, version, promise) =>
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = ChallengeSocket.Member(channel, userId, owner)
      addMember(uid, member)
      promise success ChallengeSocket.Connected(
        prependEventsSince(version, enumerator, member),
        member
      )

    case Quit(uid) => quit(uid)
  }

  override protected def broom: Unit = {
    super.broom
    if (members.nonEmpty) keepMeAlive()
  }

  protected def shouldSkipMessageFor(message: Message, member: ChallengeSocket.Member) = false
}

private object ChallengeSocket {

  case class Member(
      channel: JsChannel,
      userId: Option[String],
      owner: Boolean
  ) extends lila.socket.SocketMember {
    val troll = false
  }

  case class JoinP(uid: Uid, userId: Option[String], owner: Boolean, version: Option[SocketVersion], promise: Promise[Connected])
  case class Connected(enumerator: JsEnumerator, member: Member)

  case object Reload
}
