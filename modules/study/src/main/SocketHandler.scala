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
import lila.socket.tree.Node.{ Shape, Comment }
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
      reading[AtPosition](o) { position =>
        member.userId foreach { userId =>
          api.setPath(userId, studyId, position.ref, uid)
        }
      }
    }
    case ("deleteNode", o) => AnaRateLimit(uid.value) {
      reading[AtPosition](o) { position =>
        for {
          jumpTo <- (o \ "d" \ "jumpTo").asOpt[String] map Path.apply
          userId <- member.userId
        } api.setPath(userId, studyId, position.ref.withPath(jumpTo), uid) >>
          api.deleteNodeAt(userId, studyId, position.ref, uid)
      }
    }
    case ("promoteNode", o) => AnaRateLimit(uid.value) {
      reading[AtPosition](o) { position =>
        member.userId foreach { userId =>
          api.promoteNodeAt(userId, studyId, position.ref, uid)
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
    } api.invite(byUserId, studyId, username, socket)

    case ("kick", o) if owner => for {
      byUserId <- member.userId
      userId <- o str "d"
    } api.kick(byUserId, studyId, userId)

    case ("shapes", o) =>
      reading[AtPosition](o) { position =>
        (o \ "d" \ "shapes").asOpt[List[Shape]] foreach { shapes =>
          member.userId foreach { userId =>
            api.setShapes(userId, studyId, position.ref, shapes take 16, uid)
          }
        }
      }

    case ("addChapter", o) if owner =>
      reading[ChapterMaker.Data](o) { data =>
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

    case ("editStudy", o) if owner =>
      reading[Study.Data](o) { data =>
        member.userId foreach { byUserId =>
          api.editStudy(byUserId, studyId, data)
        }
      }

    case ("setComment", o) =>
      reading[AtPosition](o) { position =>
        for {
          userId <- member.userId
          text <- (o \ "d" \ "text").asOpt[String]
          by = (o \ "d" \ "by").asOpt[String] ifTrue owner
          comment = Comment(text = text, by = by | userId)
        } api.setComment(userId, studyId, position.ref, comment, uid)
      }
  }

  private def reading[A](o: JsValue)(f: A => Unit)(implicit reader: Reads[A]): Unit =
    o obj "d" flatMap { d => reader.reads(d).asOpt } foreach f

  private case class AtPosition(path: String, chapterId: String) {
    def ref = Position.Ref(chapterId, Path(path))
  }
  private implicit val atPositionReader = Json.reads[AtPosition]
  private case class SetRole(userId: String, role: String)
  private implicit val SetRoleReader = Json.reads[SetRole]
  private implicit val ChapterDataReader = Json.reads[ChapterMaker.Data]
  private implicit val StudyDataReader = Json.reads[Study.Data]

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
