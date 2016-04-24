package lila.study

import akka.actor._
import play.api.libs.json._
import scala.concurrent.duration._

import lila.hub.TimeBomb
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Socket.Uid
import lila.socket.{ SocketActor, History, Historical }
import lila.user.User

private final class Socket(
    studyId: String,
    lightUser: String => Option[lila.common.LightUser],
    val history: History[Socket.Messadata],
    uidTimeout: Duration,
    socketTimeout: Duration) extends SocketActor[Socket.Member](uidTimeout) with Historical[Socket.Member, Socket.Messadata] {

  import Socket._
  import JsonView._

  private val timeBomb = new TimeBomb(socketTimeout)

  private var delayedCrowdNotification = false

  def receiveSpecific = {

    case SetPath(path, uid) => notifyVersion("path", Json.obj(
      "p" -> path,
      "w" -> who(uid).map(whoWriter.writes)
    ), Messadata())

    case AddNode(pos, node, uid) => notifyVersion("addNode", Json.obj(
      "n" -> node,
      "p" -> pos,
      "w" -> who(uid)
    ), Messadata())

    case DelNode(pos, uid) => notifyVersion("delNode", Json.obj(
      "p" -> pos,
      "w" -> who(uid)
    ), Messadata())

    case ReloadMembers(members)    => notifyVersion("members", members, Messadata())

    case ReloadShapes(shapes, uid) => notifyVersion("shapes", shapes, Messadata())

    case lila.chat.actorApi.ChatLine(_, line) => line match {
      case line: lila.chat.UserLine =>
        notifyVersion("message", lila.chat.JsonView(line), Messadata(trollish = line.troll))
      case _ =>
    }

    case ReloadUser(userId) =>
      notifyIf(m => m.userId contains userId, "reload", JsNull)

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

    case Socket.Join(uid, userId, troll, owner) =>
      import play.api.libs.iteratee.Concurrent
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Socket.Member(channel, userId, troll = troll, owner = owner)
      addMember(uid.value, member)
      notifyCrowd
      sender ! Socket.Connected(enumerator, member)

    case Quit(uid) => quit(uid)

    case NotifyCrowd =>
      delayedCrowdNotification = false
      notifyAll("crowd", showSpectators(lightUser)(members.values))
  }

  def notifyCrowd {
    if (!delayedCrowdNotification) {
      delayedCrowdNotification = true
      context.system.scheduler.scheduleOnce(500 millis, self, NotifyCrowd)
    }
  }

  protected def shouldSkipMessageFor(message: Message, member: Member) =
    (message.metadata.trollish && !member.troll)

  private def who(uid: Uid) = uidToUserId(uid) map { Who(_, uid) }
}

private object Socket {

  case class Member(
    channel: JsChannel,
    userId: Option[String],
    troll: Boolean,
    owner: Boolean) extends lila.socket.SocketMember

  case class Who(u: String, s: Uid)
  import JsonView.uidWriter
  implicit private val whoWriter = Json.writes[Who]

  case class Join(uid: Uid, userId: Option[User.ID], troll: Boolean, owner: Boolean)
  case class Connected(enumerator: JsEnumerator, member: Member)

  case class ReloadUser(userId: User.ID)

  case class AddNode(position: Position.Ref, node: Node, uid: Uid)
  case class DelNode(position: Position.Ref, uid: Uid)
  case class SetPath(path: Path, uid: Uid)
  case class ReloadMembers(members: StudyMembers)
  case class ReloadShapes(shapes: List[Shape], uid: Uid)

  case class Messadata(trollish: Boolean = false)
  case object NotifyCrowd
}
