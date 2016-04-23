package lila.study

import akka.actor._
import play.api.libs.json._
import scala.concurrent.duration._

import lila.hub.TimeBomb
import lila.socket.actorApi.{ Connected => _, _ }
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

    case SetPath(path)          => notifyVersion("path", path, Messadata())

    case AddNode(pos, node)     => notifyVersion("addNode", Json.obj("n" -> node, "p" -> pos), Messadata())

    case DelNode(pos)           => notifyVersion("delNode", Json.obj("p" -> pos), Messadata())

    case ReloadMembers(members) => notifyAll("members", members)

    case ReloadShapes(shapes)   => notifyVersion("shapes", shapes, Messadata())

    case lila.chat.actorApi.ChatLine(_, line) => line match {
      case line: lila.chat.UserLine =>
        notifyVersion("message", lila.chat.JsonView(line), Messadata(line.troll))
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

    case Socket.Join(uid, userId, owner) =>
      import play.api.libs.iteratee.Concurrent
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Socket.Member(channel, userId, owner)
      addMember(uid, member)
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

  protected def shouldSkipMessageFor(message: Message, member: Socket.Member) = false
}

private object Socket {

  case class Member(
      channel: JsChannel,
      userId: Option[String],
      owner: Boolean) extends lila.socket.SocketMember {
    val troll = false
  }

  case class Join(uid: String, userId: Option[User.ID], owner: Boolean)
  case class Connected(enumerator: JsEnumerator, member: Member)

  case class ReloadUser(userId: User.ID)

  case class AddNode(position: Position.Ref, node: Node)
  case class DelNode(position: Position.Ref)
  case class SetPath(path: Path)
  case class ReloadMembers(members: StudyMembers)
  case class ReloadShapes(shapes: List[Shape])

  case class Messadata(trollish: Boolean = false)
  case object NotifyCrowd
}
