package lila.study

import actorApi.Who
import chess.Centis
import chess.format.pgn.{ Glyph, Glyphs }
import chess.format.UciPath
import play.api.libs.json.*

import lila.common.Bus
import lila.common.Json.{ *, given }
import lila.room.RoomSocket.{ Protocol as RP, * }
import lila.socket.RemoteSocket.{ Protocol as P, * }
import lila.socket.Socket.{ makeMessage, Sri }
import lila.socket.{ AnaAny, AnaDests, AnaDrop, AnaMove }
import lila.tree.Node.{ defaultNodeJsonWriter, Comment, Gamebook, Shape, Shapes }
import lila.tree.Branch
import lila.user.MyId

final private class StudySocket(
    api: StudyApi,
    jsonView: JsonView,
    remoteSocketApi: lila.socket.RemoteSocket,
    chatApi: lila.chat.ChatApi
)(using Executor, Scheduler):

  import StudySocket.{ *, given }

  lazy val rooms = makeRoomMap(send)

  subscribeChat(rooms, _.Study)

  def isPresent(studyId: StudyId, userId: UserId): Fu[Boolean] =
    lila.socket.SocketRequest[Boolean](
      id => send(Protocol.Out.getIsPresent(id, studyId, userId)),
      _ == "true"
    )

  def onServerEval(studyId: StudyId, eval: ServerEval.Progress): Unit =
    import eval.*
    import lila.game.JsonView.given
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

  private lazy val studyHandler: Handler =
    case RP.In.ChatSay(roomId, userId, msg) => api.talk(userId, roomId, msg)
    case RP.In.TellRoomSri(studyId, P.In.TellSri(sri, user, tpe, o)) =>
      import Protocol.In.{ *, given }
      import JsonView.given
      def who                      = user.map(Who(_, sri))
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
              applyWho(moveOrDrop(studyId, move, MoveOpts parse o))

        case "anaDrop" =>
          AnaDrop
            .parse(o)
            .foreach: drop =>
              applyWho(moveOrDrop(studyId, drop, MoveOpts parse o))

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
                applyWho(api.promote(studyId, position.ref, toMainline))

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
                Bus.publish(actorApi.Kick(studyId, username.id, w.myId), "kickStudy")

        case "leave" =>
          who.foreach: w =>
            api.kick(studyId, w.u, w.myId)

        case "shapes" =>
          reading[AtPosition](o): position =>
            (o \ "d" \ "shapes")
              .asOpt[List[Shape]]
              .foreach: shapes =>
                applyWho(api.setShapes(studyId, position.ref, Shapes(shapes take 32)))

        case "addChapter" =>
          reading[ChapterMaker.Data](o): data =>
            val sticky = o.obj("d").flatMap(_.boolean("sticky")) | true
            applyWho(api.addChapter(studyId, data, sticky = sticky, withRatings = true))

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
          reading[actorApi.SetTag](o): setTag =>
            applyWho(api.setTag(studyId, setTag))

        case "setComment" =>
          reading[AtPosition](o): position =>
            (o \ "d" \ "text")
              .asOpt[String]
              .foreach: text =>
                applyWho(api.setComment(studyId, position.ref, Comment sanitize text))

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
          reading[actorApi.ExplorerGame](o): data =>
            applyWho(api.explorerGame(studyId, data))

        case "requestAnalysis" =>
          o.get[StudyChapterId]("d")
            .foreach: chapterId =>
              user.foreach(api.analysisRequest(studyId, chapterId, _))

        case "invite" =>
          for
            w        <- who
            username <- o.get[UserStr]("d")
          yield InviteLimitPerUser.zero(w.u, cost = 1):
            api.invite(
              w.u,
              studyId,
              username,
              isPresent = userId => isPresent(studyId, userId),
              onError = err => send(P.Out.tellSri(w.sri, makeMessage("error", err)))
            )

        case "relaySync" =>
          applyWho: w =>
            Bus.publish(actorApi.RelayToggle(studyId, ~(o \ "d").asOpt[Boolean], w), "relayToggle")

        case t => logger.warn(s"Unhandled study socket message: $t")

  private lazy val rHandler: Handler = roomHandler(
    rooms,
    chatApi,
    logger,
    _ => _ => none, // the "talk" event is handled by the study API
    localTimeout = Some { (roomId, modId, suspectId) =>
      api.isContributor(roomId, modId) >>& !api.isMember(roomId, suspectId) >>&
        !Bus.ask("isOfficialRelay") { actorApi.IsOfficialRelay(roomId, _) }
    },
    chatBusChan = _.Study
  )

  private def moveOrDrop(studyId: StudyId, m: AnaAny, opts: MoveOpts)(who: Who) =
    m.branch.foreach: branch =>
      if branch.ply < Node.MAX_PLIES then
        m.chapterId
          .ifTrue(opts.write)
          .foreach: chapterId =>
            api.addNode(
              studyId,
              Position.Ref(chapterId, m.path),
              branch withClock opts.clock,
              opts
            )(who)

  private lazy val send: String => Unit = remoteSocketApi.makeSender("study-out").apply

  remoteSocketApi.subscribe("study-in", RP.In.reader)(
    studyHandler orElse rHandler orElse remoteSocketApi.baseHandler
  ) andDo send(P.Out.boot)

  // send API

  import JsonView.given
  import jsonView.given
  import lila.tree.Node.{ defaultNodeJsonWriter, given }
  private type SendToStudy = StudyId => Unit
  private def version[A: Writes](tpe: String, data: A): SendToStudy =
    studyId => rooms.tell(studyId into RoomId, NotifyVersion(tpe, data))
  private def notifySri[A: Writes](sri: Sri, tpe: String, data: A): SendToStudy =
    _ => send(P.Out.tellSri(sri, makeMessage(tpe, data)))

  def setPath(pos: Position.Ref, who: Who) = version("path", Json.obj("p" -> pos, "w" -> who))
  def addNode(
      pos: Position.Ref,
      node: Branch,
      variant: chess.variant.Variant,
      sticky: Boolean,
      relay: Option[Chapter.Relay],
      who: Who
  ) =
    val dests = AnaDests(variant, node.fen, pos.path.toString, pos.chapterId.some)
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
  def changeChapter(pos: Position.Ref, who: Who) = version("changeChapter", Json.obj("p" -> pos, "w" -> who))
  def updateChapter(chapterId: StudyChapterId, who: Who) =
    version("updateChapter", Json.obj("chapterId" -> chapterId, "w" -> who))
  def descChapter(chapterId: StudyChapterId, desc: Option[String], who: Who) =
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
  def setConceal(pos: Position.Ref, ply: Option[chess.Ply]) =
    version(
      "conceal",
      Json.obj(
        "p"   -> pos,
        "ply" -> ply
      )
    )
  def setTags(chapterId: StudyChapterId, tags: chess.format.pgn.Tags, who: Who) =
    version(
      "setTags",
      Json.obj(
        "chapterId" -> chapterId,
        "tags"      -> tags,
        "w"         -> who
      )
    )
  def reloadSri(sri: Sri) = notifySri(sri, "reload", JsNull)
  def reloadSriBecauseOf(sri: Sri, chapterId: StudyChapterId) =
    notifySri(sri, "reload", Json.obj("chapterId" -> chapterId))
  def validationError(error: String, sri: Sri) = notifySri(sri, "validationError", Json.obj("error" -> error))

  private val InviteLimitPerUser = lila.memo.RateLimit[UserId](
    credits = 50,
    duration = 24 hour,
    key = "study_invite.user"
  )

  api registerSocket this

object StudySocket:

  given Conversion[RoomId, StudyId] = _ into StudyId
  given Conversion[StudyId, RoomId] = _ into RoomId

  object Protocol:

    object In:
      import play.api.libs.functional.syntax.*

      def reading[A](o: JsValue)(f: A => Unit)(using reader: Reads[A]): Unit =
        o obj "d" flatMap { d =>
          reader.reads(d).asOpt
        } foreach f

      case class AtPosition(path: UciPath, chapterId: StudyChapterId):
        def ref = Position.Ref(chapterId, path)
      given Reads[AtPosition] =
        ((__ \ "path").read[UciPath] and (__ \ "ch").read[StudyChapterId])(AtPosition.apply)
      case class SetRole(userId: UserId, role: String)
      given Reads[SetRole]                  = Json.reads
      given Reads[ChapterMaker.Mode]        = optRead(ChapterMaker.Mode.apply)
      given Reads[ChapterMaker.Orientation] = stringRead(ChapterMaker.Orientation.apply)
      given Reads[Settings.UserSelection]   = optRead(Settings.UserSelection.byKey.get)
      given Reads[chess.variant.Variant] =
        optRead(key => chess.variant.Variant(chess.variant.Variant.LilaKey(key)))
      given Reads[ChapterMaker.Data]          = Json.reads
      given Reads[ChapterMaker.EditData]      = Json.reads
      given Reads[ChapterMaker.DescData]      = Json.reads
      given studyDataReads: Reads[Study.Data] = Json.reads
      given Reads[actorApi.SetTag]            = Json.reads
      given Reads[Gamebook]                   = Json.reads
      given Reads[actorApi.ExplorerGame]      = Json.reads

    object Out:
      def getIsPresent(reqId: Int, studyId: StudyId, userId: UserId) =
        s"room/present $reqId $studyId $userId"
