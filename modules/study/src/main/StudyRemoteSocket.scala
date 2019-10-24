package lila.study

import akka.actor._
import play.api.libs.json._
import scala.concurrent.duration._
import scala.concurrent.Promise

import actorApi.Who
import chess.format.pgn.Glyph
import lila.room.RoomSocket.{ Protocol => RP, _ }
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.{ Sri, makeMessage }
import lila.socket.{ AnaMove, AnaDrop, AnaAny }
import lila.tree.Node.{ Shape, Shapes, Comment, Gamebook }
import lila.user.User

private final class StudyRemoteSocket(
    api: StudyApi,
    jsonView: JsonView,
    remoteSocketApi: lila.socket.RemoteSocket,
    chat: ActorSelection,
    system: ActorSystem
) {

  import StudyRemoteSocket._

  implicit def roomIdToStudyId(roomId: RoomId) = Study.Id(roomId.value)
  implicit def studyIdToRoomId(studyId: Study.Id) = RoomId(studyId.value)

  // private val isPresent = ask[IsPresent](Protocol.In.isPresentReader)

  // def isPresent(studyId: Study.Id, userId: User.ID): Fu[Boolean] = {
  //   val promise = isPresent(studyId, userId)
  //   send(Protocol.Out.isPresent(studyId, userId))
  //   promise.future
  // }

  lazy val rooms = makeRoomMap(send, system.lilaBus)

  private lazy val handler: Handler = roomHandler(rooms, chat,
    roomId => _.Study(roomId.value).some)

  private lazy val studyHandler: Handler = {
    case Protocol.In.TellStudySri(studyId, P.In.TellSri(sri, user, tpe, o)) =>
      import Protocol.In.Data._
      def who = user map { Who(_, sri) }
      tpe match {
        case "setPath" => reading[AtPosition](o) { position =>
          who foreach api.setPath(studyId, position.ref)
        }
        case "like" => (o \ "d" \ "liked").asOpt[Boolean] foreach { v =>
          who foreach api.like(studyId, v)
        }
        case "anaMove" => AnaMove parse o foreach { move =>
          who foreach moveOrDrop(studyId, move, MoveOpts parse o)
        }
        case "anaDrop" => AnaDrop parse o foreach { drop =>
          who foreach moveOrDrop(studyId, drop, MoveOpts parse o)
        }
        case "deleteNode" => reading[AtPosition](o) { position =>
          (o \ "d" \ "jumpTo").asOpt[String] map Path.apply foreach { jumpTo =>
            who foreach api.setPath(studyId, position.ref.withPath(jumpTo))
            who foreach api.deleteNodeAt(studyId, position.ref)
          }
        }
        case "promote" => reading[AtPosition](o) { position =>
          (o \ "d" \ "toMainline").asOpt[Boolean] foreach { toMainline =>
            who foreach api.promote(studyId, position.ref, toMainline)
          }
        }
        case "forceVariation" => reading[AtPosition](o) { position =>
          (o \ "d" \ "force").asOpt[Boolean] foreach { force =>
            who foreach api.forceVariation(studyId, position.ref, force)
          }
        }
        case "setRole" => reading[SetRole](o) { d =>
          who foreach api.setRole(studyId, d.userId, d.role)
        }
        case "kick" => o str "d" foreach { username =>
          who foreach api.kick(studyId, username)
        }
        case "leave" => who foreach { w =>
          api.kick(studyId, w.u)(w)
        }
        case "shapes" => reading[AtPosition](o) { position =>
          import JsonView.shapeReader
          (o \ "d" \ "shapes").asOpt[List[Shape]] foreach { shapes =>
            who foreach api.setShapes(studyId, position.ref, Shapes(shapes take 32))
          }
        }
        case "addChapter" => reading[ChapterMaker.Data](o) { data =>
          val sticky = o.obj("d").flatMap(_.boolean("sticky")) | true
          who foreach api.addChapter(studyId, data, sticky = sticky)
        }
        case "setChapter" => o.get[Chapter.Id]("d") foreach { chapterId =>
          who foreach api.setChapter(studyId, chapterId)
        }
        case "editChapter" => reading[ChapterMaker.EditData](o) { data =>
          who foreach api.editChapter(studyId, data)
        }
        case "descStudy" => o str "d" foreach { desc =>
          who foreach api.descStudy(studyId, desc)
        }
        case "descChapter" => reading[ChapterMaker.DescData](o) { data =>
          who foreach api.descChapter(studyId, data)
        }
        case "deleteChapter" => o.get[Chapter.Id]("d") foreach { id =>
          who foreach api.deleteChapter(studyId, id)
        }
        case "clearAnnotations" => o.get[Chapter.Id]("d") foreach { id =>
          who foreach api.clearAnnotations(studyId, id)
        }
        case "sortChapters" => o.get[List[Chapter.Id]]("d") foreach { ids =>
          who foreach api.sortChapters(studyId, ids)
        }
        case "editStudy" => (o \ "d").asOpt[Study.Data] foreach { data =>
          who foreach api.editStudy(studyId, data)
        }
        case "setTag" => reading[actorApi.SetTag](o) { setTag =>
          who foreach api.setTag(studyId, setTag)
        }
        case "setComment" => reading[AtPosition](o) { position =>
          (o \ "d" \ "text").asOpt[String] foreach { text =>
            who foreach api.setComment(studyId, position.ref, Comment sanitize text)
          }
        }
        case "deleteComment" => reading[AtPosition](o) { position =>
          (o \ "d" \ "id").asOpt[String] foreach { id =>
            who foreach api.deleteComment(studyId, position.ref, Comment.Id(id))
          }
        }
        case "setGamebook" => reading[AtPosition](o) { position =>
          (o \ "d" \ "gamebook").asOpt[Gamebook].map(_.cleanUp) foreach { gamebook =>
            who foreach api.setGamebook(studyId, position.ref, gamebook)
          }
        }
        case "toggleGlyph" => reading[AtPosition](o) { position =>
          (o \ "d" \ "id").asOpt[Int] flatMap Glyph.find foreach { glyph =>
            who foreach api.toggleGlyph(studyId, position.ref, glyph)
          }
        }
        case "explorerGame" => reading[actorApi.ExplorerGame](o) { data =>
          who foreach api.explorerGame(studyId, data)
        }
        case "requestAnalysis" => o.get[Chapter.Id]("d") foreach { chapterId =>
          who foreach api.analysisRequest(studyId, chapterId)
        }
        case t => logger.warn(s"Unhandled study socket message: $t")
      }
  }

  private def moveOrDrop(studyId: Study.Id, m: AnaAny, opts: MoveOpts)(who: Who) = m.branch match {
    case scalaz.Success(branch) if branch.ply < Node.MAX_PLIES =>
      send(P.Out.tellSri(who.sri, makeMessage("node", m json branch)))
      m.chapterId.ifTrue(opts.write) foreach { chapterId =>
        api.addNode(
          studyId,
          Position.Ref(Chapter.Id(chapterId), Path(m.path)),
          Node.fromBranch(branch) withClock opts.clock,
          opts
        )(who)
      }
    case scalaz.Success(branch) =>
      send(P.Out.tellSri(who.sri, makeMessage("stepFailure", s"ply ${branch.ply}/${Node.MAX_PLIES}")))
    case scalaz.Failure(err) =>
      send(P.Out.tellSri(who.sri, makeMessage("stepFailure", err.toString)))
  }

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
      def version(tpe: String, data: JsObject) = rooms.tell(studyId.value, NotifyVersion(tpe, data))
      def notify(tpe: String, data: JsObject) = rooms.tell(studyId.value, makeMessage(tpe, data))
      msg match {
        case SetPath(pos, who) => version("path", Json.obj("p" -> pos, "w" -> who))
        case SetLiking(liking, who) => notify("liking", Json.obj("l" -> liking, "w" -> who))
      }
  }

  private val InviteLimitPerUser = new lila.memo.RateLimit[User.ID](
    credits = 50,
    duration = 24 hour,
    name = "study invites per user",
    key = "study_invite.user"
  )
}

object StudyRemoteSocket {

  object Protocol {

    object In {

      case class TellStudySri(studyId: Study.Id, tellSri: P.In.TellSri) extends P.In

      val reader: P.In.Reader = raw => RP.In.reader(raw)

      val studyReader: P.In.Reader = raw => raw.path match {
        case "tell/study/sri" => raw.get(4) {
          case arr @ Array(studyId, _, _, _) => P.In.tellSriMapper.lift(arr drop 1).flatten map {
            TellStudySri(Study.Id(studyId), _)
          }
        }
      }

      object Data {
        import lila.common.PimpedJson._
        import play.api.libs.functional.syntax._

        def reading[A](o: JsValue)(f: A => Unit)(implicit reader: Reads[A]): Unit =
          o obj "d" flatMap { d => reader.reads(d).asOpt } foreach f

        case class AtPosition(path: String, chapterId: Chapter.Id) {
          def ref = Position.Ref(chapterId, Path(path))
        }
        implicit val chapterIdReader: Reads[Chapter.Id] = stringIsoReader(Chapter.idIso)
        implicit val chapterNameReader: Reads[Chapter.Name] = stringIsoReader(Chapter.nameIso)
        implicit val atPositionReader: Reads[AtPosition] = (
          (__ \ "path").read[String] and
          (__ \ "ch").read[Chapter.Id]
        )(AtPosition.apply _)
        case class SetRole(userId: String, role: String)
        implicit val SetRoleReader: Reads[SetRole] = Json.reads[SetRole]
        implicit val ChapterDataReader: Reads[ChapterMaker.Data] = Json.reads[ChapterMaker.Data]
        implicit val ChapterEditDataReader: Reads[ChapterMaker.EditData] = Json.reads[ChapterMaker.EditData]
        implicit val ChapterDescDataReader: Reads[ChapterMaker.DescData] = Json.reads[ChapterMaker.DescData]
        implicit val StudyDataReader: Reads[Study.Data] = Json.reads[Study.Data]
        implicit val setTagReader: Reads[actorApi.SetTag] = Json.reads[actorApi.SetTag]
        implicit val gamebookReader: Reads[Gamebook] = Json.reads[Gamebook]
        implicit val explorerGame: Reads[actorApi.ExplorerGame] = Json.reads[actorApi.ExplorerGame]
      }
    }

    // case class IsPresent(roomId: RoomId, userId: User.ID, present: Boolean) extends P.In

    // val isPresentReader: P.In.Reader = raw => raw.path match {
    //   case "room/present" => raw.args.split(" ", 3) match {
    //     case Array(roomId, userId, present) => IsPresent(RoomId(roomId), userId, present).some
    //     case _ => none
    //   }
    //   case _ => none
    // }
  }

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
