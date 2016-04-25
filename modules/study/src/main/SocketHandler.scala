package lila.study

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.ask
import com.google.common.cache.LoadingCache
import play.api.libs.json._

import lila.common.PimpedJson._
import lila.hub.actorApi.map._
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Socket.makeMessage
import lila.socket.Socket.Uid
import lila.socket.{ Handler, AnaMove, AnaDests }
import lila.user.User
import makeTimeout.short

private[study] final class SocketHandler(
    hub: lila.hub.Env,
    socketHub: ActorRef,
    chat: ActorSelection,
    destCache: LoadingCache[AnaDests.Ref, AnaDests],
    api: StudyApi) {

  import Handler.AnaRateLimit
  import JsonView.shapeReader
  import lila.socket.tree.Node.openingWriter

  private def controller(
    socket: ActorRef,
    studyId: Study.ID,
    uid: Uid,
    member: Socket.Member,
    owner: Boolean): Handler.Controller = {
    case ("p", o) => o int "v" foreach { v =>
      socket ! PingVersion(uid.value, v)
    }
    case ("talk", o) => o str "d" foreach { text =>
      member.userId foreach { userId =>
        chat ! lila.chat.actorApi.UserTalk(studyId, userId, text, socket)
      }
    }
    case ("anaMove", o) => AnaRateLimit(uid.value) {
      AnaMove parse o foreach { anaMove =>
        anaMove.branch match {
          case scalaz.Success(branch) =>
            member push makeMessage("node", Json.obj(
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
              uid)
          case scalaz.Failure(err) =>
            member push makeMessage("stepFailure", err.toString)
        }
      }
    }
    case ("anaDests", o) => AnaRateLimit(uid.value) {
      member push {
        AnaDests.parse(o).map(destCache.get).fold(makeMessage("destsFailure", "Bad dests request")) { res =>
          makeMessage("dests", Json.obj(
            "dests" -> res.dests,
            "path" -> res.path
          ) ++ res.opening.?? { o =>
              Json.obj("opening" -> o)
            }
          )
        }
      }
    }
    case ("setPath", o) => AnaRateLimit(uid.value) {
      reading[AtPath](o) { d =>
        member.userId foreach { userId =>
          api.setPath(userId, studyId, Position.Ref(d.chapterId, Path(d.path)), uid)
        }
      }
    }
    case ("deleteVariation", o) => AnaRateLimit(uid.value) {
      reading[AtPath](o) { d =>
        member.userId foreach { userId =>
          api.deleteNodeAt(userId, studyId, Position.Ref(d.chapterId, Path(d.path)), uid)
        }
      }
    }
    case ("promoteVariation", o) => AnaRateLimit(uid.value) {
      reading[AtPath](o) { d =>
        member.userId foreach { userId =>
          api.promoteNodeAt(userId, studyId, Position.Ref(d.chapterId, Path(d.path)), uid)
        }
      }
    }
    case ("setRole", o) if owner => AnaRateLimit(uid.value) {
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
          api.setShapes(userId, studyId, shapes, uid)
        }
      }

    case ("addChapter", o) if owner =>
      reading[Chapter.FormData](o) { data =>
        member.userId foreach { byUserId =>
          api.addChapter(byUserId, studyId, data, socket)
        }
      }

    case ("setChapter", o) => for {
      byUserId <- member.userId
      chapterId <- o str "d"
    } api.setChapter(byUserId, studyId, chapterId, socket)

    case ("renameChapter", o) if owner => for {
      byUserId <- member.userId
      d <- o obj "d"
      id <- d str "id"
      name <- d str "name"
    } api.renameChapter(byUserId, studyId, id, Chapter toName name)

    case ("deleteChapter", o) if owner => for {
      byUserId <- member.userId
      id <- o str "d"
    } api.deleteChapter(byUserId, studyId, id, socket)

  }

  private def reading[A](o: JsValue)(f: A => Unit)(implicit reader: Reads[A]): Unit =
    o obj "d" flatMap { d => reader.reads(d).asOpt } foreach f

  private case class AtPath(path: String, chapterId: String)
  private implicit val atPathReader = Json.reads[AtPath]
  private case class SetRole(userId: String, role: String)
  private implicit val SetRoleReader = Json.reads[SetRole]
  private implicit val ChapterDataReader = Json.reads[Chapter.FormData]

  def join(
    studyId: Study.ID,
    uid: Uid,
    user: Option[User],
    owner: Boolean): Fu[Option[JsSocketHandler]] = for {
    socket ← socketHub ? Get(studyId) mapTo manifest[ActorRef]
    join = Socket.Join(uid = uid, userId = user.map(_.id), troll = user.??(_.troll), owner = owner)
    handler ← Handler(hub, socket, uid.value, join, user.map(_.id)) {
      case Socket.Connected(enum, member) =>
        (controller(socket, studyId, uid, member, owner = owner), enum, member)
    }
  } yield handler.some
}
