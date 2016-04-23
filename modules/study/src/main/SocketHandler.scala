package lila.study

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.ask
import play.api.libs.json._

import lila.common.PimpedJson._
import lila.hub.actorApi.map._
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Socket.Uid
import lila.socket.{ Handler, AnaMove }
import lila.user.User
import makeTimeout.short

private[study] final class SocketHandler(
    hub: lila.hub.Env,
    socketHub: ActorRef,
    chat: ActorSelection,
    api: StudyApi) {

  def join(
    studyId: Study.ID,
    uid: Uid,
    user: Option[User],
    owner: Boolean): Fu[Option[JsSocketHandler]] = for {
    socket ← socketHub ? Get(studyId) mapTo manifest[ActorRef]
    join = Socket.Join(uid = uid, userId = user.map(_.id), troll = user.??(_.troll), owner = owner)
    handler ← Handler(hub, socket, uid.value, join, user.map(_.id)) {
      case Socket.Connected(enum, member) =>
        (controller(socket, studyId, member, owner = owner), enum, member)
    }
  } yield handler.some

  private def reading[A](o: JsValue)(f: A => Unit)(implicit reader: Reads[A]): Unit =
    o obj "d" flatMap { d => reader.reads(d).asOpt } foreach f

  private case class AtPath(path: String, chapterId: String)
  private implicit val atPathReader = Json.reads[AtPath]
  private case class SetRole(userId: String, role: String)
  private implicit val SetRoleReader = Json.reads[SetRole]

  import Handler.AnaRateLimit
  import JsonView.shapeReader

  private def controller(
    socket: ActorRef,
    studyId: Study.ID,
    member: Socket.Member,
    owner: Boolean): Handler.Controller = {
    case ("p", o) => o int "v" foreach { v =>
      socket ! PingVersion(member.uid.value, v)
    }
    case ("talk", o) => o str "d" foreach { text =>
      member.userId foreach { userId =>
        chat ! lila.chat.actorApi.UserTalk(studyId, userId, text, socket)
      }
    }
    case ("anaMove", o) => AnaRateLimit(member.uid.value) {
      AnaMove parse o foreach { anaMove =>
        anaMove.branch match {
          case scalaz.Success(branch) =>
            member push lila.socket.Socket.makeMessage("node", Json.obj(
              "node" -> branch,
              "path" -> anaMove.path
            ))
            for {
              userId <- member.userId
              d ← o obj "d"
              chapterId <- d str "chapterId"
            } api.addNode(
              studyId,
              Position.Ref(chapterId, Path(anaMove.path)),
              Node.fromBranchBy(userId)(branch),
              member.uid)
          case scalaz.Failure(err) =>
            member push lila.socket.Socket.makeMessage("stepFailure", err.toString)
        }
      }
    }
    case ("setPath", o) => AnaRateLimit(member.uid.value) {
      reading[AtPath](o) { d =>
        member.userId foreach { userId =>
          api.setPath(userId, studyId, Position.Ref(d.chapterId, Path(d.path)), member.uid)
        }
      }
    }
    case ("deleteVariation", o) => AnaRateLimit(member.uid.value) {
      reading[AtPath](o) { d =>
        member.userId foreach { userId =>
          api.deleteNodeAt(userId, studyId, Position.Ref(d.chapterId, Path(d.path)), member.uid)
        }
      }
    }
    case ("promoteVariation", o) => AnaRateLimit(member.uid.value) {
      reading[AtPath](o) { d =>
        member.userId foreach { userId =>
          api.promoteNodeAt(userId, studyId, Position.Ref(d.chapterId, Path(d.path)), member.uid)
        }
      }
    }
    case ("setRole", o) if owner => AnaRateLimit(member.uid.value) {
      reading[SetRole](o) { d =>
        member.userId foreach { userId =>
          api.setRole(userId, studyId, d.userId, d.role)
        }
      }
    }
    case ("invite", o) if owner => for {
      byUserId <- member.userId
      username <- o str "d"
    } api.invite(byUserId, studyId, username)

    case ("kick", o) if owner => for {
      byUserId <- member.userId
      userId <- o str "d"
    } api.kick(byUserId, studyId, userId)

    case ("shapes", o) =>
      (o \ "d").asOpt[List[Shape]] foreach { shapes =>
        member.userId foreach { userId =>
          api.setShapes(userId, studyId, shapes, member.uid)
        }
      }
  }
}
