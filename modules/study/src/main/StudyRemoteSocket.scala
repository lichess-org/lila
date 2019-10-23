package lila.study

import akka.actor._
import play.api.libs.json._
import scala.concurrent.duration._
import scala.concurrent.Promise

import actorApi.Who
import lila.room.RoomSocket.{ Protocol => RP, _ }
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.{ Sri, makeMessage }
import lila.user.User

private final class StudyRemoteSocket(
    api: StudyApi,
    jsonView: JsonView,
    remoteSocketApi: lila.socket.RemoteSocket,
    chat: ActorSelection,
    system: ActorSystem
) {

  implicit def roomIdToStudyId(roomId: RoomId) = Study.Id(roomId.value)

  lazy val rooms = makeRoomMap(send, system.lilaBus)

  private lazy val handler: Handler = roomHandler(rooms, chat,
    roomId => _.Study(roomId.value).some)

  private lazy val studyHandler: Handler = {
    case m @ RP.In.TellRoomSri(roomId, P.In.TellSri(sri, user, tpe, msg)) if messagesHandled(tpe) =>
      import Protocol.In.Data._
      def who = user map { Who(_, sri) }
      (tpe -> msg) match {
        case ("setPath", o) => reading[AtPosition](o) { position =>
          who foreach { api.setPath(_, roomId, position.ref) }
        }
        case ("like", o) => for {
          w <- who
          v <- (o \ "d" \ "liked").asOpt[Boolean]
        } api.like(w, roomId, v)
      }
  }

  private val messagesHandled: Set[String] =
    Set("like", "setPath")

  private lazy val send: String => Unit = remoteSocketApi.makeSender("study-out").apply _

  remoteSocketApi.subscribe("study-in", Protocol.In.reader)(
    studyHandler orElse handler orElse remoteSocketApi.baseHandler
  )

  system.lilaBus.subscribeFun('studySocket) {
    case StudyRemoteSocket.Send(studyId, msg) =>
      import StudyRemoteSocket.Out._
      import JsonView._
      import jsonView.membersWrites
      import lila.tree.Node.{ openingWriter, commentWriter, glyphsWriter, shapesWrites, clockWrites }
      msg match {
        case SetPath(pos, who) => tellRoom(studyId, NotifyVersion("path", Json.obj(
          "p" -> pos,
          "w" -> who
        )))
        case SetLiking(liking, who) => tellRoom(studyId, makeMessage("liking", Json.obj(
          "l" -> liking,
          "w" -> who
        )))
      }
  }

  private def tellRoom(studyId: Study.Id, msg: Any): Unit =
    rooms.tell(studyId.value, msg)

  object Protocol {

    object In {

      val reader: P.In.Reader = raw => RP.In.reader(raw)

      // val studyReader: P.In.Reader = raw => raw.path match {
      //   case _ => none
      // }

      object Data {
        import lila.common.PimpedJson._
        import play.api.libs.functional.syntax._

        def reading[A](o: JsValue)(f: A => Unit)(implicit reader: Reads[A]): Unit =
          o obj "d" flatMap { d => reader.reads(d).asOpt } foreach f

        case class AtPosition(path: String, chapterId: Chapter.Id) {
          def ref = Position.Ref(chapterId, Path(path))
        }
        implicit val chapterIdReader = stringIsoReader(Chapter.idIso)
        implicit val chapterNameReader = stringIsoReader(Chapter.nameIso)
        implicit val atPositionReader: Reads[AtPosition] = (
          (__ \ "path").read[String] and
          (__ \ "ch").read[Chapter.Id]
        )(AtPosition.apply _)
        case class SetRole(userId: String, role: String)
        implicit val SetRoleReader = Json.reads[SetRole]
        implicit val ChapterDataReader = Json.reads[ChapterMaker.Data]
        implicit val ChapterEditDataReader = Json.reads[ChapterMaker.EditData]
        implicit val ChapterDescDataReader = Json.reads[ChapterMaker.DescData]
        implicit val StudyDataReader = Json.reads[Study.Data]
        implicit val setTagReader = Json.reads[actorApi.SetTag]
        // implicit val gamebookReader = Json.reads[Gamebook]
        implicit val explorerGame = Json.reads[actorApi.ExplorerGame]
      }
    }

    object Out {
    }
  }
}

object StudyRemoteSocket {

  sealed trait Out

  object Out {
    // case class AddNode(
    //     position: Position.Ref,
    //     node: Node,
    //     variant: chess.variant.Variant,
    //     sri: Sri,
    //     sticky: Boolean,
    //     relay: Option[Chapter.Relay]
    // ) extends Out
    // case class DeleteNode(position: Position.Ref, sri: Sri) extends Out
    // case class Promote(position: Position.Ref, toMainline: Boolean, sri: Sri) extends Out
    case class SetPath(position: Position.Ref, who: Who) extends Out
    case class SetLiking(liking: Study.Liking, who: Who) extends Out
    // case class SetShapes(position: Position.Ref, shapes: Shapes, sri: Sri)
    // case class ReloadMembers(members: StudyMembers)
    // case class SetComment(position: Position.Ref, comment: Comment, sri: Sri)
    // case class DeleteComment(position: Position.Ref, commentId: Comment.Id, sri: Sri)
    // case class SetGlyphs(position: Position.Ref, glyphs: Glyphs, sri: Sri)
    // case class SetClock(position: Position.Ref, clock: Option[Centis], sri: Sri)
    // case class ForceVariation(position: Position.Ref, force: Boolean, sri: Sri)
    // case class ReloadChapters(chapters: List[Chapter.Metadata])
    // case object ReloadAll
    // case class ChangeChapter(sri: Sri, position: Position.Ref)
    // case class UpdateChapter(sri: Sri, chapterId: Chapter.Id)
    // case class DescChapter(sri: Sri, chapterId: Chapter.Id, desc: Option[String])
    // case class DescStudy(sri: Sri, desc: Option[String])
    // case class AddChapter(sri: Sri, position: Position.Ref, sticky: Boolean)
    // case class ValidationError(sri: Sri, error: String)
    // case class SetConceal(position: Position.Ref, ply: Option[Chapter.Ply])
    // case class SetTags(chapterId: Chapter.Id, tags: chess.format.pgn.Tags, sri: Sri)
    // case class Broadcast(t: String, msg: JsObject)
  }

  case class Send(studyId: Study.Id, msg: Out)
}
