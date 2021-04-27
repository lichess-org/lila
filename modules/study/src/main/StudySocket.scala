package lila.study

import actorApi.Who
import cats.data.Validated
import chess.Centis
import chess.format.pgn.{ Glyph, Glyphs }
import play.api.libs.json._
import scala.concurrent.duration._

import lila.common.Bus
import lila.room.RoomSocket.{ Protocol => RP, _ }
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.{ makeMessage, Sri }
import lila.socket.{ AnaAny, AnaDests, AnaDrop, AnaMove }
import lila.tree.Node.{ defaultNodeJsonWriter, Comment, Gamebook, Shape, Shapes }
import lila.user.User

final private class StudySocket(
    api: StudyApi,
    jsonView: JsonView,
    remoteSocketApi: lila.socket.RemoteSocket,
    chatApi: lila.chat.ChatApi
)(implicit
    ec: scala.concurrent.ExecutionContext,
    mode: play.api.Mode
) {

  import StudySocket._

  implicit def roomIdToStudyId(roomId: RoomId)    = Study.Id(roomId.value)
  implicit def studyIdToRoomId(studyId: Study.Id) = RoomId(studyId.value)

  lazy val rooms = makeRoomMap(send)

  subscribeChat(rooms, _.Study)

  def isPresent(studyId: Study.Id, userId: User.ID): Fu[Boolean] =
    remoteSocketApi.request[Boolean](
      id => send(Protocol.Out.getIsPresent(id, studyId, userId)),
      _ == "true"
    )

  def onServerEval(studyId: Study.Id, eval: ServerEval.Progress): Unit =
    eval match {
      case ServerEval.Progress(chapterId, tree, analysis, division) =>
        import lila.game.JsonView.divisionWriter
        import JsonView._
        send(
          RP.Out.tellRoom(
            studyId,
            makeMessage(
              "analysisProgress",
              Json.obj(
                "analysis" -> analysis,
                "ch"       -> chapterId,
                "tree"     -> defaultNodeJsonWriter.writes(tree),
                "division" -> division
              )
            )
          )
        )
    }

  private lazy val studyHandler: Handler = {
    case RP.In.ChatSay(roomId, userId, msg) => api.talk(userId, roomId, msg)
    case RP.In.TellRoomSri(studyId, P.In.TellSri(sri, user, tpe, o)) =>
      import Protocol.In.Data._
      import JsonView.shapeReader
      def who = user map { Who(_, sri) }
      tpe match {
        case "setPath" =>
          reading[AtPosition](o) { position =>
            who foreach api.setPath(studyId, position.ref)
          }
        case "like" =>
          (o \ "d" \ "liked").asOpt[Boolean] foreach { v =>
            who foreach api.like(studyId, v)
          }
        case "anaMove" =>
          AnaMove parse o foreach { move =>
            who foreach moveOrDrop(studyId, move, MoveOpts parse o)
          }
        case "anaDrop" =>
          AnaDrop parse o foreach { drop =>
            who foreach moveOrDrop(studyId, drop, MoveOpts parse o)
          }
        case "deleteNode" =>
          reading[AtPosition](o) { position =>
            (o \ "d" \ "jumpTo").asOpt[String] map Path.apply foreach { jumpTo =>
              who foreach api.setPath(studyId, position.ref.withPath(jumpTo))
              who foreach api.deleteNodeAt(studyId, position.ref)
            }
          }
        case "promote" =>
          reading[AtPosition](o) { position =>
            (o \ "d" \ "toMainline").asOpt[Boolean] foreach { toMainline =>
              who foreach api.promote(studyId, position.ref, toMainline)
            }
          }
        case "forceVariation" =>
          reading[AtPosition](o) { position =>
            (o \ "d" \ "force").asOpt[Boolean] foreach { force =>
              who foreach api.forceVariation(studyId, position.ref, force)
            }
          }
        case "setRole" =>
          reading[SetRole](o) { d =>
            who foreach api.setRole(studyId, d.userId, d.role)
          }
        case "kick" =>
          o str "d" foreach { username =>
            who foreach api.kick(studyId, username)
          }
        case "leave" =>
          who foreach { w =>
            api.kick(studyId, w.u)(w)
          }
        case "shapes" =>
          reading[AtPosition](o) { position =>
            (o \ "d" \ "shapes").asOpt[List[Shape]] foreach { shapes =>
              who foreach api.setShapes(studyId, position.ref, Shapes(shapes take 32))
            }
          }
        case "addChapter" =>
          reading[ChapterMaker.Data](o) { data =>
            val sticky = o.obj("d").flatMap(_.boolean("sticky")) | true
            who foreach api.addChapter(studyId, data, sticky = sticky)
          }
        case "setChapter" =>
          o.get[Chapter.Id]("d") foreach { chapterId =>
            who foreach api.setChapter(studyId, chapterId)
          }
        case "editChapter" =>
          reading[ChapterMaker.EditData](o) { data =>
            who foreach api.editChapter(studyId, data)
          }
        case "descStudy" =>
          o str "d" foreach { desc =>
            who foreach api.descStudy(studyId, desc)
          }
        case "descChapter" =>
          reading[ChapterMaker.DescData](o) { data =>
            who foreach api.descChapter(studyId, data)
          }
        case "deleteChapter" =>
          o.get[Chapter.Id]("d") foreach { id =>
            who foreach api.deleteChapter(studyId, id)
          }
        case "clearAnnotations" =>
          o.get[Chapter.Id]("d") foreach { id =>
            who foreach api.clearAnnotations(studyId, id)
          }
        case "sortChapters" =>
          o.get[List[Chapter.Id]]("d") foreach { ids =>
            who foreach api.sortChapters(studyId, ids)
          }
        case "editStudy" =>
          (o \ "d").asOpt[Study.Data] foreach { data =>
            who foreach api.editStudy(studyId, data)
          }
        case "setTag" =>
          reading[actorApi.SetTag](o) { setTag =>
            who foreach api.setTag(studyId, setTag)
          }
        case "setComment" =>
          reading[AtPosition](o) { position =>
            (o \ "d" \ "text").asOpt[String] foreach { text =>
              who foreach api.setComment(studyId, position.ref, Comment sanitize text)
            }
          }
        case "deleteComment" =>
          reading[AtPosition](o) { position =>
            (o \ "d" \ "id").asOpt[String] foreach { id =>
              who foreach api.deleteComment(studyId, position.ref, Comment.Id(id))
            }
          }
        case "setGamebook" =>
          reading[AtPosition](o) { position =>
            (o \ "d" \ "gamebook").asOpt[Gamebook].map(_.cleanUp) foreach { gamebook =>
              who foreach api.setGamebook(studyId, position.ref, gamebook)
            }
          }
        case "toggleGlyph" =>
          reading[AtPosition](o) { position =>
            (o \ "d" \ "id").asOpt[Int] flatMap Glyph.find foreach { glyph =>
              who foreach api.toggleGlyph(studyId, position.ref, glyph)
            }
          }
        case "setTopics" =>
          o strs "d" foreach { topics =>
            who foreach api.setTopics(studyId, topics)
          }
        case "explorerGame" =>
          reading[actorApi.ExplorerGame](o) { data =>
            who foreach api.explorerGame(studyId, data)
          }
        case "requestAnalysis" =>
          o.get[Chapter.Id]("d") foreach { chapterId =>
            user foreach { api.analysisRequest(studyId, chapterId, _) }
          }
        case "invite" =>
          for {
            w        <- who
            username <- o str "d"
          } InviteLimitPerUser(w.u, cost = 1) {
            api.invite(
              w.u,
              studyId,
              username,
              isPresent = userId => isPresent(studyId, userId),
              onError = err => send(P.Out.tellSri(w.sri, makeMessage("error", err)))
            )
          }(funit)
        case "relaySync" =>
          who foreach { w =>
            Bus.publish(actorApi.RelayToggle(studyId, ~(o \ "d").asOpt[Boolean], w), "relayToggle")
          }
        case t => logger.warn(s"Unhandled study socket message: $t")
      }
  }

  private lazy val rHandler: Handler = roomHandler(
    rooms,
    chatApi,
    logger,
    _ => _ => none, // the "talk" event is handled by the study API
    localTimeout = Some { (roomId, modId, suspectId) =>
      api.isContributor(roomId, modId) >>& !api.isMember(roomId, suspectId)
    },
    chatBusChan = _.Study
  )

  private def moveOrDrop(studyId: Study.Id, m: AnaAny, opts: MoveOpts)(who: Who) =
    m.branch match {
      case Validated.Valid(branch) if branch.ply < Node.MAX_PLIES =>
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

  remoteSocketApi.subscribe("study-in", RP.In.reader)(
    studyHandler orElse rHandler orElse remoteSocketApi.baseHandler
  ) >>- send(P.Out.boot)

  // send API

  import JsonView._
  import jsonView.membersWrites
  import lila.tree.Node.{
    clockWrites,
    commentWriter,
    defaultNodeJsonWriter,
    glyphsWriter,
    openingWriter,
    shapesWrites
  }
  private type SendToStudy = Study.Id => Unit
  private def version[A: Writes](tpe: String, data: A): SendToStudy =
    studyId => rooms.tell(studyId.value, NotifyVersion(tpe, data))
  private def notify[A: Writes](tpe: String, data: A): SendToStudy =
    studyId => send(RP.Out.tellRoom(studyId, makeMessage(tpe, data)))
  private def notifySri[A: Writes](sri: Sri, tpe: String, data: A): SendToStudy =
    _ => send(P.Out.tellSri(sri, makeMessage(tpe, data)))

  def setPath(pos: Position.Ref, who: Who) = version("path", Json.obj("p" -> pos, "w" -> who))
  def addNode(
      pos: Position.Ref,
      node: Node,
      variant: chess.variant.Variant,
      sticky: Boolean,
      relay: Option[Chapter.Relay],
      who: Who
  ) = {
    val dests = AnaDests(variant, node.fen, pos.path.toString, pos.chapterId.value.some)
    version(
      "addNode",
      Json
        .obj(
          "n" -> defaultNodeJsonWriter.writes(TreeBuilder.toBranch(node, variant)),
          "p" -> pos,
          "w" -> who,
          "d" -> dests.dests,
          "o" -> dests.opening,
          "s" -> sticky
        )
        .add("relay", relay)
    )
  }
  def deleteNode(pos: Position.Ref, who: Who) = version("deleteNode", Json.obj("p" -> pos, "w" -> who))
  def promote(pos: Position.Ref, toMainline: Boolean, who: Who) =
    version(
      "promote",
      Json.obj(
        "p"          -> pos,
        "toMainline" -> toMainline,
        "w"          -> who
      )
    )
  def setLiking(liking: Study.Liking, who: Who) = notify("liking", Json.obj("l" -> liking, "w" -> who))
  def setShapes(pos: Position.Ref, shapes: Shapes, who: Who) =
    version(
      "shapes",
      Json.obj(
        "p" -> pos,
        "s" -> shapes,
        "w" -> who
      )
    )
  def reloadMembers(members: StudyMembers, sendTo: Iterable[User.ID])(studyId: Study.Id) =
    send(RP.Out.tellRoomUsers(studyId, sendTo, makeMessage("members", members)))

  def setComment(pos: Position.Ref, comment: Comment, who: Who) =
    version(
      "setComment",
      Json.obj(
        "p" -> pos,
        "c" -> comment,
        "w" -> who
      )
    )
  def deleteComment(pos: Position.Ref, commentId: Comment.Id, who: Who) =
    version(
      "deleteComment",
      Json.obj(
        "p"  -> pos,
        "id" -> commentId,
        "w"  -> who
      )
    )
  def setGlyphs(pos: Position.Ref, glyphs: Glyphs, who: Who) =
    version(
      "glyphs",
      Json.obj(
        "p" -> pos,
        "g" -> glyphs,
        "w" -> who
      )
    )
  def setClock(pos: Position.Ref, clock: Option[Centis], who: Who) =
    version(
      "clock",
      Json.obj(
        "p" -> pos,
        "c" -> clock,
        "w" -> who
      )
    )
  def forceVariation(pos: Position.Ref, force: Boolean, who: Who) =
    version(
      "forceVariation",
      Json.obj(
        "p"     -> pos,
        "force" -> force,
        "w"     -> who
      )
    )
  private[study] def reloadChapters(chapters: List[Chapter.Metadata]) = version("chapters", chapters)
  def reloadAll                                                       = version("reload", JsNull)
  def changeChapter(pos: Position.Ref, who: Who)                      = version("changeChapter", Json.obj("p" -> pos, "w" -> who))
  def updateChapter(chapterId: Chapter.Id, who: Who) =
    version("updateChapter", Json.obj("chapterId" -> chapterId, "w" -> who))
  def descChapter(chapterId: Chapter.Id, desc: Option[String], who: Who) =
    version(
      "descChapter",
      Json.obj(
        "chapterId" -> chapterId,
        "desc"      -> desc,
        "w"         -> who
      )
    )
  def descStudy(desc: Option[String], who: Who) = version("descStudy", Json.obj("desc" -> desc, "w" -> who))
  def setTopics(topics: StudyTopics, who: Who) =
    version("setTopics", Json.obj("topics" -> topics, "w" -> who))
  def addChapter(pos: Position.Ref, sticky: Boolean, who: Who) =
    version(
      "addChapter",
      Json.obj(
        "p" -> pos,
        "w" -> who,
        "s" -> sticky
      )
    )
  def setConceal(pos: Position.Ref, ply: Option[Chapter.Ply]) =
    version(
      "conceal",
      Json.obj(
        "p"   -> pos,
        "ply" -> ply.map(_.value)
      )
    )
  def setTags(chapterId: Chapter.Id, tags: chess.format.pgn.Tags, who: Who) =
    version(
      "setTags",
      Json.obj(
        "chapterId" -> chapterId,
        "tags"      -> tags,
        "w"         -> who
      )
    )
  def reloadSri(sri: Sri) = notifySri(sri, "reload", JsNull)
  def reloadSriBecauseOf(sri: Sri, chapterId: Chapter.Id) =
    notifySri(sri, "reload", Json.obj("chapterId" -> chapterId))
  def validationError(error: String, sri: Sri) = notifySri(sri, "validationError", Json.obj("error" -> error))

  private val InviteLimitPerUser = new lila.memo.RateLimit[User.ID](
    credits = 50,
    duration = 24 hour,
    key = "study_invite.user"
  )

  api registerSocket this
}

object StudySocket {

  object Protocol {

    object In {

      object Data {
        import lila.common.Json._
        import play.api.libs.functional.syntax._

        def reading[A](o: JsValue)(f: A => Unit)(implicit reader: Reads[A]): Unit =
          o obj "d" flatMap { d =>
            reader.reads(d).asOpt
          } foreach f

        case class AtPosition(path: String, chapterId: Chapter.Id) {
          def ref = Position.Ref(chapterId, Path(path))
        }
        implicit val chapterIdReader: Reads[Chapter.Id]     = stringIsoReader(Chapter.idIso)
        implicit val chapterNameReader: Reads[Chapter.Name] = stringIsoReader(Chapter.nameIso)
        implicit val atPositionReader: Reads[AtPosition] = (
          (__ \ "path").read[String] and
            (__ \ "ch").read[Chapter.Id]
        )(AtPosition.apply _)
        case class SetRole(userId: String, role: String)
        implicit val SetRoleReader: Reads[SetRole]                       = Json.reads[SetRole]
        implicit val ChapterDataReader: Reads[ChapterMaker.Data]         = Json.reads[ChapterMaker.Data]
        implicit val ChapterEditDataReader: Reads[ChapterMaker.EditData] = Json.reads[ChapterMaker.EditData]
        implicit val ChapterDescDataReader: Reads[ChapterMaker.DescData] = Json.reads[ChapterMaker.DescData]
        implicit val StudyDataReader: Reads[Study.Data]                  = Json.reads[Study.Data]
        implicit val setTagReader: Reads[actorApi.SetTag]                = Json.reads[actorApi.SetTag]
        implicit val gamebookReader: Reads[Gamebook]                     = Json.reads[Gamebook]
        implicit val explorerGame: Reads[actorApi.ExplorerGame]          = Json.reads[actorApi.ExplorerGame]
      }
    }

    object Out {
      def getIsPresent(reqId: Int, studyId: Study.Id, userId: User.ID) =
        s"room/present $reqId $studyId $userId"
    }
  }
}
