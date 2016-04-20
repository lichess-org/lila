package lila.study

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.ask
import play.api.libs.json._

import lila.common.PimpedJson._
import lila.hub.actorApi.map._
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.{ Handler, AnaMove }
import lila.user.User
import makeTimeout.short

private[study] final class SocketHandler(
    hub: lila.hub.Env,
    socketHub: ActorRef,
    api: StudyApi) {

  def join(
    studyId: Study.ID,
    uid: String,
    userId: Option[User.ID],
    owner: Boolean): Fu[Option[JsSocketHandler]] = for {
    socket ← socketHub ? Get(studyId) mapTo manifest[ActorRef]
    join = Socket.Join(uid = uid, userId = userId, owner = owner)
    handler ← Handler(hub, socket, uid, join, userId) {
      case Socket.Connected(enum, member) =>
        (controller(socket, studyId, uid, member), enum, member)
    }
  } yield handler.some

  private def reading[A](o: JsValue)(f: A => Unit)(implicit reader: Reads[A]): Unit =
    o obj "d" flatMap { d => reader.reads(d).asOpt } foreach f

  private case class AtPath(path: String, chapterId: String)
  private implicit val atPathVarReader = Json.reads[AtPath]

  import Handler.AnaRateLimit

  private def controller(
    socket: ActorRef,
    studyId: Study.ID,
    uid: String,
    member: Socket.Member): Handler.Controller = {
    case ("p", o) => o int "v" foreach { v =>
      socket ! PingVersion(uid, v)
    }
    case ("anaMove", o) => AnaRateLimit(uid) {
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
              Location.Ref(studyId, chapterId),
              Path(anaMove.path),
              Node.fromBranchBy(userId)(branch))
          case scalaz.Failure(err) =>
            member push lila.socket.Socket.makeMessage("stepFailure", err.toString)
        }
      }
    }
    case ("setPos", o) => AnaRateLimit(uid) {
      reading[AtPath](o) { d =>
        member.userId foreach { userId =>
          api.setMemberPosition(userId, Location.Ref(studyId, d.chapterId), Path(d.path))
        }
      }
    }
    case ("deleteVariation", o) => AnaRateLimit(uid) {
      reading[AtPath](o) { d =>
        member.userId foreach { userId =>
          api.deleteNodeAt(userId, Location.Ref(studyId, d.chapterId), Path(d.path))
        }
      }
    }
    case ("promoteVariation", o) => AnaRateLimit(uid) {
      reading[AtPath](o) { d =>
        member.userId foreach { userId =>
          api.promoteNodeAt(userId, Location.Ref(studyId, d.chapterId), Path(d.path))
        }
      }
    }
  }
}
