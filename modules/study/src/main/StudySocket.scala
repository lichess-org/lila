package lila.study

import chess.Centis
import chess.format.UciPath
import chess.format.pgn.{ Glyph, Glyphs }
import play.api.libs.json.*
import scalalib.actor.SyncActorMap

import lila.common.Bus
import lila.common.Json.{ *, given }
import lila.room.RoomSocket.{ Protocol as RP, * }
import lila.core.socket.{ protocol as P, * }
import lila.tree.Branch
import lila.tree.Node.{ Comment, Gamebook, Shape, Shapes }
import lila.tree.Node.minimalNodeJsonWriter
import lila.core.study.Visibility
import cats.mtl.Handle.*

final private class StudySocket(
    api: StudyApi,
    jsonView: JsonView,
    socketKit: SocketKit,
    socketRequest: SocketRequester,
    chatApi: lila.core.chat.ChatApi
)(using Executor, Scheduler):

  import StudySocket.{ *, given }

  lazy val rooms: SyncActorMap[RoomId, RoomState] = makeRoomMap(send)

  subscribeChat(rooms, _.study)

  def isPresent(studyId: StudyId, userId: UserId): Fu[Boolean] =
    socketRequest[Boolean](
      id => send.exec(Protocol.Out.getIsPresent(id, studyId, userId)),
      _ == "true"
    )

  def onServerEval(studyId: StudyId, eval: ServerEval.Progress): Unit =
    import eval.*
    send.exec(
      RP.Out.tellRoom(
        studyId,
        makeMessage(
          "analysisProgress",
          Json.obj(
            "analysis" -> analysis,
            "ch" -> chapterId,
            "tree" -> lila.tree.Node.defaultNodeJsonWriter.writes(tree),
            "division" -> division
          )
        )
      )
    )

  private lazy val studyHandler: SocketHandler =
    case RP.In.ChatSay(roomId, userId, msg) => api.talk(userId, roomId, msg)
    case RP.In.TellRoomSri(studyId, P.In.TellSri(sri, user, tpe, o)) =>
      import Protocol.In.{ *, given }
      import JsonView.given
      def who = user.map(Who(_, sri))
      def applyWho(f: Who => Unit) = who.foreach(f)

      tpe match
        case "setPath" =>
          reading[AtPosition](o): position =>
            applyWho(api.setPath(studyId, position.ref))

        case "like" =>
          (o \ "d" \ "liked")
            .asOpt[Boolean]
            .foreach: v =>
              applyWho(api.like(studyId, v))

        case "anaMove" =>
          AnaMove
            .parse(o)
            .foreach: move =>
              applyWho(moveOrDrop(studyId, move, MoveOpts.parse(o))(using _))

        case "anaDrop" =>
          AnaDrop
            .parse(o)
            .foreach: drop =>
              applyWho(moveOrDrop(studyId, drop, MoveOpts.parse(o))(using _))

        case "deleteNode" =>
          reading[AtPosition](o): position =>
            (o \ "d" \ "jumpTo")
              .asOpt[UciPath]
              .foreach: jumpTo =>
                applyWho(api.setPath(studyId, position.ref.withPath(jumpTo)))
                applyWho(api.deleteNodeAt(studyId, position.ref))

        case "promote" =>
          reading[AtPosition](o): position =>
            (o \ "d" \ "toMainline")
              .asOpt[Boolean]
              .foreach: toMainline =>
                applyWho(api.promote(studyId, position.ref, toMainline)(using _))

        case "forceVariation" =>
          reading[AtPosition](o): position =>
            (o \ "d" \ "force")
              .asOpt[Boolean]
              .foreach: force =>
                applyWho(api.forceVariation(studyId, position.ref, force))

        case "setRole" =>
          reading[SetRole](o): d =>
            applyWho(api.setRole(studyId, d.userId, d.role))

        case "kick" =>
          o.get[UserStr]("d")
            .foreach: username =>
              applyWho: w =>
                api.kick(studyId, username.id, w.myId)
                Bus.pub(Kick(studyId, username.id, w.myId))

        case "leave" =>
          who.foreach: w =>
            api.kick(studyId, w.u, w.myId)

        case "shapes" =>
          reading[AtPosition](o): position =>
            (o \ "d" \ "shapes")
              .asOpt[List[Shape]]
              .foreach: shapes =>
                applyWho(api.setShapes(studyId, position.ref, Shapes(shapes.take(32))))

        case "addChapter" =>
          reading[ChapterMaker.Data](o): data =>
            val sticky = o.obj("d").flatMap(_.boolean("sticky")) | true
            val withRatings = o.obj("d").flatMap(_.boolean("showRatings")) | true
            who.foreach: w =>
              allow:
                api.addChapter(studyId, data, sticky = sticky, withRatings = withRatings)(w)
              .rescue: err =>
                fuccess(send.exec(P.Out.tellSri(w.sri, makeMessage("error", err))))

        case "setChapter" =>
          o.get[StudyChapterId]("d")
            .foreach: chapterId =>
              applyWho(api.setChapter(studyId, chapterId))

        case "editChapter" =>
          reading[ChapterMaker.EditData](o): data =>
            applyWho(api.editChapter(studyId, data))

        case "descStudy" =>
          o.str("d")
            .foreach: desc =>
              applyWho(api.descStudy(studyId, desc))

        case "descChapter" =>
          reading[ChapterMaker.DescData](o): data =>
            applyWho(api.descChapter(studyId, data))

        case "deleteChapter" =>
          o.get[StudyChapterId]("d")
            .foreach: id =>
              applyWho(api.deleteChapter(studyId, id))

        case "clearAnnotations" =>
          o.get[StudyChapterId]("d")
            .foreach: id =>
              applyWho(api.clearAnnotations(studyId, id))

        case "clearVariations" =>
          o.get[StudyChapterId]("d")
            .foreach: id =>
              applyWho(api.clearVariations(studyId, id))

        case "sortChapters" =>
          o.get[List[StudyChapterId]]("d")
            .foreach: ids =>
              applyWho(api.sortChapters(studyId, ids))

        case "editStudy" =>
          (o \ "d")
            .asOpt[Study.Data]
            .foreach: data =>
              applyWho(api.editStudy(studyId, data))

        case "setTag" =>
          reading[SetTag](o): setTag =>
            applyWho(api.setTag(studyId, setTag))

        case "setComment" =>
          reading[AtPosition](o): position =>
            (o \ "d" \ "text")
              .asOpt[String]
              .foreach: text =>
                val commentId = (o \ "d" \ "id").asOpt[String].map(Comment.Id.apply)
                applyWho(api.setComment(studyId, position.ref, commentId, Comment.sanitize(text)))

        case "deleteComment" =>
          reading[AtPosition](o): position =>
            (o \ "d" \ "id")
              .asOpt[String]
              .foreach: id =>
                applyWho(api.deleteComment(studyId, position.ref, Comment.Id(id)))

        case "setGamebook" =>
          reading[AtPosition](o): position =>
            (o \ "d" \ "gamebook")
              .asOpt[Gamebook]
              .map(_.cleanUp)
              .foreach: gamebook =>
                applyWho(api.setGamebook(studyId, position.ref, gamebook))

        case "toggleGlyph" =>
          reading[AtPosition](o): position =>
            (o \ "d" \ "id")
              .asOpt[Int]
              .flatMap(Glyph.find)
              .foreach: glyph =>
                applyWho(api.toggleGlyph(studyId, position.ref, glyph))

        case "setTopics" =>
          o.strs("d")
            .foreach: topics =>
              applyWho(api.setTopics(studyId, topics))

        case "explorerGame" =>
          reading[ExplorerGame](o): data =>
            applyWho(api.explorerGame(studyId, data))

        case "requestAnalysis" =>
          o.get[StudyChapterId]("d")
            .foreach: chapterId =>
              user.foreach(api.analysisRequest(studyId, chapterId, _))

        case "invite" =>
          for
            w <- who
            username <- o.get[UserStr]("d")
          yield api
            .invite(w.u, studyId, username, isPresent(studyId, _))
            .recover:
              case err: Exception => send.exec(P.Out.tellSri(w.sri, makeMessage("error", err.getMessage)))

        case "relaySync" =>
          applyWho: w =>
            Bus.pub(RelayToggle(studyId, ~(o \ "d").asOpt[Boolean], w))

        case t => logger.warn(s"Unhandled study socket message: $t")

  private lazy val rHandler: SocketHandler = roomHandler(
    rooms,
    chatApi,
    logger,
    _ => _ => none, // the "talk" event is handled by the study API
    localTimeout = Some { (roomId, modId, suspectId) =>
      api.isContributor(roomId, modId) >>& api.isMember(roomId, suspectId).not >>&
        Bus.ask[Boolean, IsOfficialRelay](IsOfficialRelay(roomId, _)).not
    },
    chatBusChan = _.study
  )

  private def moveOrDrop(studyId: StudyId, m: AnaAny, opts: MoveOpts)(using Who) =
    m.branch.foreach: branch =>
      if branch.ply < Node.MAX_PLIES then
        m.chapterId
          .ifTrue(opts.write)
          .foreach: chapterId =>
            api.addNode(AddNode(studyId, Position.Ref(chapterId, m.path), branch, opts))

  private lazy val send = socketKit.send("study-out")

  socketKit
    .subscribe("study-in", RP.In.reader)(studyHandler.orElse(rHandler).orElse(socketKit.baseHandler))
    .andDo(send.exec(P.Out.boot))

  // send API

  import JsonView.given
  import jsonView.given
  import lila.tree.Node.given
  private type SendToStudy = StudyId => Unit
  private def version[A: Writes](tpe: String, data: A): SendToStudy =
    studyId => rooms.tell(studyId.into(RoomId), NotifyVersion(tpe, data))
  private def notifySri[A: Writes](sri: Sri, tpe: String, data: A): SendToStudy =
    _ => send.exec(P.Out.tellSri(sri, makeMessage(tpe, data)))

  def setPath(pos: Position.Ref, who: Who) = version("path", Json.obj("p" -> pos, "w" -> who))
  def addNode(
      pos: Position.Ref,
      node: Branch,
      variant: chess.variant.Variant,
      sticky: Boolean,
      relay: Option[Chapter.Relay],
      who: Who
  ) =
    AnaDests(variant, node.fen, pos.path.toString, pos.chapterId.some)
    val relayPathDedup = relay
      .map(_.path)
      .map: path =>
        if path == pos.path.+(node.id) then "!"
        else path.toString
    version(
      "addNode",
      Json
        .obj(
          "n" -> minimalNodeJsonWriter.writes(node),
          "p" -> pos,
          "s" -> sticky
        )
        .add("w", Option.when(relay.isEmpty)(who))
        .add("relayPath", relayPathDedup)
    )
  def deleteNode(pos: Position.Ref, who: Who) = version("deleteNode", Json.obj("p" -> pos, "w" -> who))
  def promote(pos: Position.Ref, toMainline: Boolean, who: Who) =
    version(
      "promote",
      Json.obj(
        "p" -> pos,
        "toMainline" -> toMainline,
        "w" -> who
      )
    )
  def setLiking(liking: Study.Liking, who: Who) =
    notifySri(who.sri, "liking", Json.obj("l" -> liking, "w" -> who))
  def setShapes(pos: Position.Ref, shapes: Shapes, who: Who) =
    version(
      "shapes",
      Json.obj(
        "p" -> pos,
        "s" -> shapes,
        "w" -> who
      )
    )
  def reloadMembers(members: StudyMembers, sendTo: Iterable[UserId])(studyId: StudyId) =
    send.exec(RP.Out.tellRoomUsers(studyId, sendTo, makeMessage("members", members)))

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
        "p" -> pos,
        "id" -> commentId,
        "w" -> who
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
  def setClock(pos: Position.Ref, clock: Option[Centis], relayDenorm: Option[Chapter.BothClocks]) =
    version(
      "clock",
      Json
        .obj(
          "p" -> pos,
          "c" -> clock
        )
        .add(
          "relayClocks",
          relayDenorm.map: clocks =>
            Json.arr(clocks.white, clocks.black)
        )
    )
  def forceVariation(pos: Position.Ref, force: Boolean, who: Who) =
    version(
      "forceVariation",
      Json.obj(
        "p" -> pos,
        "force" -> force,
        "w" -> who
      )
    )
  private[study] def reloadChapters(previews: ChapterPreview.AsJsons) = version("chapters", previews)
  def reloadAll = version("reload", JsNull)
  def changeChapter(pos: Position.Ref, who: Who) = version("changeChapter", Json.obj("p" -> pos, "w" -> who))
  def updateChapter(chapterId: StudyChapterId, who: Who) =
    version("updateChapter", Json.obj("chapterId" -> chapterId, "w" -> who))
  def descChapter(chapterId: StudyChapterId, desc: Option[String], who: Who) =
    version(
      "descChapter",
      Json.obj(
        "chapterId" -> chapterId,
        "desc" -> desc,
        "w" -> who
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
  def setConceal(pos: Position.Ref, ply: Option[chess.Ply]) =
    version(
      "conceal",
      Json.obj(
        "p" -> pos,
        "ply" -> ply
      )
    )
  def setTags(chapterId: StudyChapterId, tags: chess.format.pgn.Tags, who: Who) =
    version(
      "setTags",
      Json.obj(
        "chapterId" -> chapterId,
        "tags" -> tags,
        "w" -> who
      )
    )
  def reloadSri(sri: Sri) = notifySri(sri, "reload", JsNull)
  def reloadSriBecauseOf(sri: Sri, chapterId: StudyChapterId, reason: Option["overweight"]) =
    notifySri(sri, "reload", Json.obj("chapterId" -> chapterId).add("reason" -> reason))
  def validationError(error: String, sri: Sri) = notifySri(sri, "validationError", Json.obj("error" -> error))

  api.registerSocket(this)

object StudySocket:

  given Conversion[RoomId, StudyId] = _.into(StudyId)
  given Conversion[StudyId, RoomId] = _.into(RoomId)

  object Protocol:

    object In:
      import play.api.libs.functional.syntax.*

      def reading[A](o: JsValue)(f: A => Unit)(using reader: Reads[A]): Unit =
        o.obj("d")
          .flatMap: d =>
            reader.reads(d).asOpt
          .foreach(f)

      case class AtPosition(path: UciPath, chapterId: StudyChapterId):
        def ref = Position.Ref(chapterId, path)
      given Reads[AtPosition] =
        ((__ \ "path").read[UciPath].and((__ \ "ch").read[StudyChapterId]))(AtPosition.apply)
      case class SetRole(userId: UserId, role: String)
      given Reads[SetRole] = Json.reads
      given Reads[ChapterMaker.Mode] = optRead(ChapterMaker.Mode.apply)
      given Reads[ChapterMaker.Orientation] = stringRead(ChapterMaker.Orientation.apply)
      given Reads[Settings.UserSelection] = optRead(Settings.UserSelection.byKey.get)
      given Reads[chess.variant.Variant] =
        optRead(key => chess.variant.Variant(chess.variant.Variant.LilaKey(key)))
      given Reads[ChapterMaker.Data] = Json.reads
      given Reads[ChapterMaker.EditData] = Json.reads
      given Reads[ChapterMaker.DescData] = Json.reads
      given Reads[Visibility] = stringRead(v => Visibility.byKey.getOrElse(v, Visibility.public))
      given studyDataReads: Reads[Study.Data] = Json.reads
      given Reads[SetTag] = Json.reads
      given Reads[Gamebook] = Json.reads
      given Reads[ExplorerGame] = Json.reads

    object Out:
      def getIsPresent(reqId: Int, studyId: StudyId, userId: UserId) =
        s"room/present $reqId $studyId $userId"
