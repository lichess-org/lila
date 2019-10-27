package lila.study

import akka.actor._
import java.util.concurrent.ConcurrentHashMap
import play.api.libs.json._
import scala.concurrent.duration._
import scala.concurrent.Promise

import actorApi.Who
import chess.Centis
import chess.format.pgn.{ Glyph, Glyphs }
import lila.room.RoomSocket.{ Protocol => RP, _ }
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.{ Sri, makeMessage }
import lila.socket.{ AnaMove, AnaDrop, AnaAny, SocketTrouper, History, Historical, AnaDests, DirectSocketMember }
import lila.tree.Node.{ Shape, Shapes, Comment, Gamebook }
import lila.user.User

private final class StudySocket(
    api: StudyApi,
    jsonView: JsonView,
    lightStudyCache: LightStudyCache,
    remoteSocketApi: lila.socket.RemoteSocket,
    chat: ActorSelection,
    bus: lila.common.Bus
) {

  import StudySocket._

  implicit def roomIdToStudyId(roomId: RoomId) = Study.Id(roomId.value)
  implicit def studyIdToRoomId(studyId: Study.Id) = RoomId(studyId.value)

  lazy val rooms = makeRoomMap(send, bus)

  def isPresent(studyId: Study.Id, userId: User.ID): Fu[Boolean] =
    remoteSocketApi.request[Boolean](
      id => send(Protocol.Out.getIsPresent(id, studyId, userId)),
      _ == "true"
    )

  def onServerEval(studyId: Study.Id, eval: ServerEval.Progress): Unit = eval match {
    case ServerEval.Progress(chapterId, tree, analysis, division) =>
      import lila.game.JsonView.divisionWriter
      import JsonView._
      send(RP.Out.tellRoom(studyId, makeMessage("analysisProgress", Json.obj(
        "analysis" -> analysis,
        "ch" -> chapterId,
        "tree" -> tree,
        "division" -> division
      ))))
  }

  private lazy val studyHandler: Handler = {
    case Protocol.In.TellStudySri(studyId, P.In.TellSri(sri, user, tpe, o)) =>
      import Protocol.In.Data._
      import JsonView.shapeReader
      def who = user map { Who(_, sri) }
      tpe match {
        case "talk" => o str "d" foreach { text =>
          user foreach { api.talk(_, studyId, text) }
        }
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
          user foreach { api.analysisRequest(studyId, chapterId, _) }
        }
        case "invite" => for {
          w <- who
          username <- o str "d"
        } InviteLimitPerUser(w.u, cost = 1) {
          api.invite(w.u, studyId, username,
            isPresent = userId => isPresent(studyId, userId).thenPp(s"isPresent response $studyId, $username"),
            onError = err => send(P.Out.tellSri(w.sri, makeMessage("error", err))))
        }
        case "relaySync" => who foreach { w =>
          bus.publish(actorApi.RelayToggle(studyId, ~(o \ "d").asOpt[Boolean], w), 'relayToggle)
        }
        case t => logger.warn(s"Unhandled study socket message: $t")
      }
    case Protocol.In.StudyDoor(moves) => moves foreach {
      case (userId, through) =>
        val studyId = through.fold(identity, identity)
        lightStudyCache get studyId foreach {
          _ foreach { study =>
            bus.publish(lila.hub.actorApi.study.StudyDoor(
              userId = userId,
              studyId = studyId.value,
              contributor = study contributors userId,
              public = study.isPublic,
              enters = through.isRight
            ), 'study)
          }
        }
    }
  }

  private lazy val rHandler: Handler = roomHandler(rooms, chat,
    roomId => _ => none, // the "talk" event is handled by the study API
    canTimeout = Some { (roomId, modId, suspectId) =>
      api.isContributor(roomId, modId) >>& !api.isMember(roomId, suspectId)
    })

  private def moveOrDrop(studyId: Study.Id, m: AnaAny, opts: MoveOpts)(who: Who) = m.branch match {
    case scalaz.Success(branch) if branch.ply < Node.MAX_PLIES =>
      m.chapterId.ifTrue(opts.write) foreach { chapterId =>
        api.addNode(
          studyId,
          Position.Ref(Chapter.Id(chapterId), Path(m.path)),
          Node.fromBranch(branch) withClock opts.clock,
          opts
        )(who)
      }
    case _ =>
  }

  private lazy val send: String => Unit = remoteSocketApi.makeSender("study-out").apply _

  remoteSocketApi.subscribe("study-in", Protocol.In.reader)(
    studyHandler orElse rHandler orElse remoteSocketApi.baseHandler
  )

  bus.subscribeFun('studySocket) {
    case Send(studyId, msg) =>
      import Out._
      import JsonView._
      import jsonView.membersWrites
      import lila.tree.Node.{ openingWriter, commentWriter, glyphsWriter, shapesWrites, clockWrites }
      def version[A: Writes](tpe: String, data: A) = rooms.tell(studyId.value, NotifyVersion(tpe, data))
      def notify[A: Writes](tpe: String, data: A) = send(RP.Out.tellRoom(studyId, makeMessage(tpe, data)))
      def notifySri[A: Writes](sri: Sri, tpe: String, data: A) = send(P.Out.tellSri(sri, makeMessage(tpe, data)))
      msg match {
        case SetPath(pos, who) => version("path", Json.obj("p" -> pos, "w" -> who))

        case SetLiking(liking, who) => notify("liking", Json.obj("l" -> liking, "w" -> who))

        case AddNode(pos, node, variant, sticky, relay, who) =>
          val dests = AnaDests(variant, node.fen, pos.path.toString, pos.chapterId.value.some)
          version("addNode", Json.obj(
            "n" -> TreeBuilder.toBranch(node, variant),
            "p" -> pos,
            "w" -> who,
            "d" -> dests.dests,
            "o" -> dests.opening,
            "s" -> sticky
          ).add("relay", relay))

        case DeleteNode(pos, who) => version("deleteNode", Json.obj("p" -> pos, "w" -> who))

        case Promote(pos, toMainline, who) => version("promote", Json.obj(
          "p" -> pos,
          "toMainline" -> toMainline,
          "w" -> who
        ))
        case ReloadMembers(studyMembers) =>
          version("members", studyMembers)
          send(RP.Out.tellRoomUsers(studyId, studyMembers.ids, makeMessage("reload")))

        case ReloadChapters(chapters) => version("chapters", chapters)

        case ReloadAll => version("reload", JsNull)

        case ChangeChapter(pos, who) => version("changeChapter", Json.obj("p" -> pos, "w" -> who))

        case UpdateChapter(chapterId, who) => version("updateChapter", Json.obj("chapterId" -> chapterId, "w" -> who))

        case DescChapter(chapterId, description, who) => version("descChapter", Json.obj(
          "chapterId" -> chapterId,
          "desc" -> description,
          "w" -> who
        ))

        case DescStudy(description, who) => version("descStudy", Json.obj("desc" -> description, "w" -> who))

        case AddChapter(pos, sticky, who) => version("addChapter", Json.obj(
          "p" -> pos,
          "w" -> who,
          "s" -> sticky
        ))

        case ValidationError(error, sri) => notifySri(sri, "validationError", Json.obj("error" -> error))

        case SetShapes(pos, shapes, who) => version("shapes", Json.obj(
          "p" -> pos,
          "s" -> shapes,
          "w" -> who
        ))
        case SetComment(pos, comment, who) => version("setComment", Json.obj(
          "p" -> pos,
          "c" -> comment,
          "w" -> who
        ))

        case SetTags(chapterId, tags, who) => version("setTags", Json.obj(
          "chapterId" -> chapterId,
          "tags" -> tags,
          "w" -> who
        ))

        case DeleteComment(pos, commentId, who) => version("deleteComment", Json.obj(
          "p" -> pos,
          "id" -> commentId,
          "w" -> who
        ))

        case SetGlyphs(pos, glyphs, who) => version("glyphs", Json.obj(
          "p" -> pos,
          "g" -> glyphs,
          "w" -> who
        ))

        case SetClock(pos, clock, who) => version("clock", Json.obj(
          "p" -> pos,
          "c" -> clock,
          "w" -> who
        ))

        case ForceVariation(pos, force, who) => version("forceVariation", Json.obj(
          "p" -> pos,
          "force" -> force,
          "w" -> who
        ))

        case SetConceal(pos, ply) => version("conceal", Json.obj(
          "p" -> pos,
          "ply" -> ply.map(_.value)
        ))

        case ReloadSri(sri) => notifySri(sri, "reload", JsNull)

        case ReloadSriBecauseOf(sri, chapterId) => notifySri(sri, "reload", Json.obj("chapterId" -> chapterId))
      }
  }

  private val InviteLimitPerUser = new lila.memo.RateLimit[User.ID](
    credits = 50,
    duration = 24 hour,
    name = "study invites per user",
    key = "study_invite.user"
  )
}

object StudySocket {

  object Protocol {

    object In {

      case class TellStudySri(studyId: Study.Id, tellSri: P.In.TellSri) extends P.In
      case class StudyDoor(through: Map[User.ID, Either[Study.Id, Study.Id]]) extends P.In

      val reader: P.In.Reader = raw => studyReader(raw) orElse RP.In.reader(raw)

      val studyReader: P.In.Reader = raw => raw.path match {
        case "tell/study/sri" => raw.get(4) {
          case arr @ Array(studyId, _, _, _) => P.In.tellSriMapper.lift(arr drop 1).flatten map {
            TellStudySri(Study.Id(studyId), _)
          }
        }
        case "study/door" => Some(StudyDoor {
          P.In.commas(raw.args).map(_ split ":").collect {
            case Array(u, s, "+") => u -> Right(Study.Id(s))
            case Array(u, s, "-") => u -> Left(Study.Id(s))
          }(scala.collection.breakOut)
        })
        case _ => none
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

    object Out {
      def getIsPresent(reqId: Int, studyId: Study.Id, userId: User.ID) =
        s"room/present $reqId $studyId $userId"
    }
  }

  sealed trait Out

  object Out {
    case class AddNode(
        position: Position.Ref,
        node: Node,
        variant: chess.variant.Variant,
        sticky: Boolean,
        relay: Option[Chapter.Relay],
        who: Who
    ) extends Out
    case class ReloadSri(sri: Sri) extends Out
    case class ReloadSriBecauseOf(sri: Sri, chapterId: Chapter.Id) extends Out
    case class DeleteNode(position: Position.Ref, who: Who) extends Out
    case class Promote(position: Position.Ref, toMainline: Boolean, who: Who) extends Out
    case class SetPath(position: Position.Ref, who: Who) extends Out
    case class SetLiking(liking: Study.Liking, who: Who) extends Out
    case class SetShapes(position: Position.Ref, shapes: Shapes, who: Who) extends Out
    case class ReloadMembers(members: StudyMembers) extends Out
    case class SetComment(position: Position.Ref, comment: Comment, who: Who) extends Out
    case class DeleteComment(position: Position.Ref, commentId: Comment.Id, who: Who) extends Out
    case class SetGlyphs(position: Position.Ref, glyphs: Glyphs, who: Who) extends Out
    case class SetClock(position: Position.Ref, clock: Option[Centis], who: Who) extends Out
    case class ForceVariation(position: Position.Ref, force: Boolean, who: Who) extends Out
    case class ReloadChapters(chapters: List[Chapter.Metadata]) extends Out
    case object ReloadAll extends Out
    case class ChangeChapter(position: Position.Ref, who: Who) extends Out
    case class UpdateChapter(chapterId: Chapter.Id, who: Who) extends Out
    case class DescChapter(chapterId: Chapter.Id, desc: Option[String], who: Who) extends Out
    case class DescStudy(desc: Option[String], who: Who) extends Out
    case class AddChapter(position: Position.Ref, sticky: Boolean, who: Who) extends Out
    case class SetConceal(position: Position.Ref, ply: Option[Chapter.Ply]) extends Out
    case class SetTags(chapterId: Chapter.Id, tags: chess.format.pgn.Tags, who: Who) extends Out
    case class ValidationError(error: String, sri: Sri) extends Out
  }

  case class Send(studyId: Study.Id, msg: Out)
}
