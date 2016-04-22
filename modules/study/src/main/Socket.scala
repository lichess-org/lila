package lila.study

import akka.actor._
import play.api.libs.json._
import scala.concurrent.duration.Duration

import lila.hub.TimeBomb
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.{ SocketActor, History, Historical }
import lila.user.User

private final class Socket(
    studyId: String,
    val history: History[Unit],
    getStudy: Study.ID => Fu[Option[Study]],
    uidTimeout: Duration,
    socketTimeout: Duration) extends SocketActor[Socket.Member](uidTimeout) with Historical[Socket.Member, Unit] {

  import Socket._
  import JsonView._

  private val timeBomb = new TimeBomb(socketTimeout)

  def receiveSpecific = {

    case MemberPosition(userId, pos) => notifyIf(
      m => !m.userId.contains(userId),
      "mpos",
      Json.obj("u" -> userId, "p" -> pos))

    case AddNode(pos, node) => notifyIf(
      m => !m.userId.contains(node.by),
      "addNode",
      Json.obj("n" -> node, "p" -> pos))

    case DelNode(pos) => notifyAll(
      "delNode",
      Json.obj("p" -> pos))

    case ReloadMembers(members) => notifyAll(
      "reloadMembers", members)

    case ReloadMemberShapes(userId, shapes) => notifyAll(
      "reloadMemberShapes",
      Json.obj("u" -> userId, "shapes" -> shapes))

    case Reload =>
      getStudy(studyId) foreach {
        _ foreach { study =>
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
      import play.api.libs.iteratee.Concurrent
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Socket.Member(channel, userId, owner)
      addMember(uid, member)
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

  case class Join(uid: String, userId: Option[String], owner: Boolean)
  case class Connected(enumerator: JsEnumerator, member: Member)

  case object Reload

  case class MemberPosition(userId: User.ID, position: Position.Ref)
  case class AddNode(position: Position.Ref, node: Node)
  case class DelNode(position: Position.Ref)
  case class ReloadMembers(members: StudyMembers)
  case class ReloadMemberShapes(userId: User.ID, shapes: List[Shape])
}
