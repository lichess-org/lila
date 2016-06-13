package lila.study

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.ask
import chess.format.pgn.{ Glyph, Glyphs }
import com.google.common.cache.LoadingCache
import play.api.libs.json._

import lila.common.PimpedJson._
import lila.hub.actorApi.map._
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Socket.makeMessage
import lila.socket.Socket.Uid
import lila.socket.tree.Node.{ Shape, Shapes, Comment }
import lila.socket.{ Handler, AnaMove, AnaDests, AnaDrop }
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
    owner: Boolean): Handler.Controller = ({
    case ("p", o) => o int "v" foreach { v =>
      socket ! PingVersion(uid.value, v)
    }
    case ("talk", o) => o str "d" foreach { text =>
      member.userId foreach { userId =>
        api.talk(userId, studyId, text, socket)
      }
    }
    case ("anaMove", o) => AnaRateLimit(uid.value) {
      AnaMove parse o foreach { anaMove =>
        anaMove.branch match {
          case scalaz.Success(branch) if branch.ply < Node.MAX_PLIES =>
            member push makeMessage("node", Json.obj(
              "node" -> branch,
              "path" -> anaMove.path
            ))
            for {
              userId <- member.userId
              d ← o obj "d"
              chapterId <- d str "chapterId"
            } api.addNode(
              userId,
              studyId,
              Position.Ref(chapterId, Path(anaMove.path)),
              Node.fromBranch(branch),
              uid)
          case scalaz.Success(branch) =>
            member push makeMessage("stepFailure", s"ply ${branch.ply}/${Node.MAX_PLIES}")
          case scalaz.Failure(err) =>
            member push makeMessage("stepFailure", err.toString)
        }
      }
    }
    case ("anaDrop", o) => AnaRateLimit(uid.value) {
      AnaDrop parse o foreach { anaDrop =>
        anaDrop.branch match {
          case scalaz.Success(branch) if branch.ply < Node.MAX_PLIES =>
            member push makeMessage("node", Json.obj(
              "node" -> branch,
              "path" -> anaDrop.path
            ))
            for {
              userId <- member.userId
              d ← o obj "d"
              chapterId <- d str "chapterId"
            } api.addNode(
              userId,
              studyId,
              Position.Ref(chapterId, Path(anaDrop.path)),
              Node.fromBranch(branch),
              uid)
          case scalaz.Success(branch) =>
            member push makeMessage("stepFailure", s"ply ${branch.ply}/${Node.MAX_PLIES}")
          case scalaz.Failure(err) =>
            member push lila.socket.Socket.makeMessage("stepFailure", err.toString)
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

    case ("kick", o) if owner   => o str "d" foreach { api.kick(studyId, _) }

    case ("leave", _) if !owner => member.userId foreach { api.kick(studyId, _) }

    case ("shapes", o) =>
      reading[AtPosition](o) { position =>
        (o \ "d" \ "shapes").asOpt[List[Shape]] foreach { shapes =>
          member.userId foreach { userId =>
            api.setShapes(userId, studyId, position.ref, Shapes(shapes take 16), uid)
          }
        }
      }

    case ("addChapter", o) =>
      reading[ChapterMaker.Data](o) { data =>
        member.userId foreach { byUserId =>
          api.addChapter(byUserId, studyId, data, socket, uid)
        }
      }

    case ("setChapter", o) => for {
      byUserId <- member.userId
      chapterId <- o str "d"
    } api.setChapter(byUserId, studyId, chapterId, socket, uid)

    case ("editChapter", o) =>
      reading[ChapterMaker.EditData](o) { data =>
        member.userId foreach { byUserId =>
          api.editChapter(byUserId, studyId, data, socket, uid)
        }
      }

    case ("deleteChapter", o) => for {
      byUserId <- member.userId
      id <- o str "d"
    } api.deleteChapter(byUserId, studyId, id, socket, uid)

    case ("sortChapters", o) => for {
      byUserId <- member.userId
      ids <- o strs "d"
    } api.sortChapters(byUserId, studyId, ids, socket, uid)

    case ("editStudy", o) if owner =>
      reading[Study.Data](o) { data =>
        api.editStudy(studyId, data)
      }

    case ("setComment", o) =>
      reading[AtPosition](o) { position =>
        for {
          userId <- member.userId
          text <- (o \ "d" \ "text").asOpt[String]
        } api.setComment(userId, studyId, position.ref, Comment sanitize text, uid)
      }

    case ("deleteComment", o) =>
      reading[AtPosition](o) { position =>
        for {
          userId <- member.userId
          id <- (o \ "d" \ "id").asOpt[String]
        } api.deleteComment(userId, studyId, position.ref, Comment.Id(id), uid)
      }

    case ("toggleGlyph", o) =>
      reading[AtPosition](o) { position =>
        for {
          userId <- member.userId
          by = (o \ "d" \ "by").asOpt[String] ifTrue owner
          glyph <- (o \ "d" \ "id").asOpt[Int] flatMap Glyph.find
        } api.toggleGlyph(userId, studyId, position.ref, glyph, uid)
      }

    case ("like", o) => for {
      byUserId <- member.userId
      v <- (o \ "d" \ "liked").asOpt[Boolean]
    } api.like(studyId, byUserId, v, socket, uid)
  }: Handler.Controller) orElse lila.chat.Socket.in(
    chatId = studyId,
    member = member,
    socket = socket,
    chat = chat)

  private def reading[A](o: JsValue)(f: A => Unit)(implicit reader: Reads[A]): Unit =
    o obj "d" flatMap { d => reader.reads(d).asOpt } foreach f

  private case class AtPosition(path: String, chapterId: String) {
    def ref = Position.Ref(chapterId, Path(path))
  }
  private implicit val atPositionReader = Json.reads[AtPosition]
  private case class SetRole(userId: String, role: String)
  private implicit val SetRoleReader = Json.reads[SetRole]
  private implicit val ChapterDataReader = Json.reads[ChapterMaker.Data]
  private implicit val ChapterEditDataReader = Json.reads[ChapterMaker.EditData]
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
