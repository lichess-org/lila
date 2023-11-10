package lila.study

import chess.format.pgn.{ PgnStr, Tags }
import chess.format.Fen
import chess.variant.Variant
import lila.chat.ChatApi
import lila.game.{ Game, Namer }
import chess.Color
import lila.tree.{ Root, Branch, Branches }

final private class ChapterMaker(
    net: lila.common.config.NetConfig,
    lightUser: lila.user.LightUserApi,
    chatApi: ChatApi,
    gameRepo: lila.game.GameRepo,
    pgnFetch: PgnFetch,
    pgnDump: lila.game.PgnDump
)(using Executor):

  import ChapterMaker.*

  def apply(study: Study, data: Data, order: Int, userId: UserId, withRatings: Boolean): Fu[Chapter] =
    data.game.so(parseGame) flatMap {
      case None =>
        data.game so pgnFetch.fromUrl flatMap {
          case Some(pgn) => fromFenOrPgnOrBlank(study, data.copy(pgn = pgn.some), order, userId)
          case _         => fromFenOrPgnOrBlank(study, data, order, userId)
        }
      case Some(game) => fromGame(study, game, data, order, userId, withRatings)
    } map { (c: Chapter) =>
      if c.name.value.isEmpty then c.copy(name = Chapter defaultName order) else c
    }

  def fromFenOrPgnOrBlank(study: Study, data: Data, order: Int, userId: UserId): Fu[Chapter] =
    data.pgn.filter(_.value.trim.nonEmpty) match
      case Some(pgn) => fromPgn(study, pgn, data, order, userId)
      case None      => fuccess(fromFenOrBlank(study, data, order, userId))

  private def fromPgn(study: Study, pgn: PgnStr, data: Data, order: Int, userId: UserId): Fu[Chapter] =
    for
      contributors <- lightUser.asyncMany(study.members.contributorIds.toList)
      parsed <- PgnImport(pgn, contributors.flatten).toFuture recoverWith { case e: Exception =>
        fufail(ValidationException(e.getMessage))
      }
    yield Chapter.make(
      studyId = study.id,
      name = getChapterNameFromPgn(data, parsed),
      setup = Chapter.Setup(
        none,
        parsed.variant,
        resolveOrientation(data, parsed.root, userId, parsed.tags)
      ),
      root = parsed.root,
      tags = parsed.tags,
      order = order,
      ownerId = userId,
      practice = data.isPractice,
      gamebook = data.isGamebook,
      conceal = data.isConceal option parsed.root.ply
    )

  private def getChapterNameFromPgn(data: Data, parsed: PgnImport.Result): StudyChapterName =
    def vsFromPgnTags = for
      white <- parsed.tags(_.White)
      black <- parsed.tags(_.Black)
    yield s"$white - $black"
    data.name.some
      .ifFalse(data.isDefaultName)
      .orElse:
        StudyChapterName.from:
          vsFromPgnTags.orElse(parsed.tags("Event")).filter(_.nonEmpty)
      .getOrElse(data.name)

  private def resolveOrientation(data: Data, root: Root, userId: UserId, tags: Tags = Tags.empty): Color =
    def isMe(name: Option[String]) = name.flatMap(UserStr.read).exists(_.id == userId)
    data.orientation match
      case Orientation.Fixed(color)      => color
      case _ if isMe(tags.players.white) => Color.white
      case _ if isMe(tags.players.black) => Color.black
      // if an outcome is known, then it's a finished game, which we show from white perspective by convention
      case _ if tags.outcome.isDefined => Color.white
      // in gamebooks (interactive chapter), we guess the orientation based on the last node
      case _ if data.isGamebook => !root.lastMainlineNode.color
      // else we show from the perspective of whoever turn it is to move
      case _ => root.lastMainlineNode.color

  private def fromFenOrBlank(study: Study, data: Data, order: Int, userId: UserId): Chapter =
    val variant = data.variant | Variant.default
    val (root, isFromFen) =
      data.fen.filterNot(_.isInitial).flatMap { Fen.readWithMoveNumber(variant, _) } match
        case Some(sit) =>
          Root(
            ply = sit.ply,
            fen = Fen write sit,
            check = sit.situation.check,
            clock = none,
            crazyData = sit.situation.board.crazyData,
            children = Branches.empty
          ) -> true
        case None => Root.default(variant) -> false
    Chapter.make(
      studyId = study.id,
      name = data.name,
      setup = Chapter.Setup(
        none,
        variant,
        resolveOrientation(data, root, userId),
        fromFen = isFromFen option true
      ),
      root = root,
      tags = Tags.empty,
      order = order,
      ownerId = userId,
      practice = data.isPractice,
      gamebook = data.isGamebook,
      conceal = data.isConceal option root.ply
    )

  private[study] def fromGame(
      study: Study,
      game: Game,
      data: Data,
      order: Int,
      userId: UserId,
      withRatings: Boolean,
      initialFen: Option[Fen.Epd] = None
  ): Fu[Chapter] =
    for
      root <- makeRoot(game, data.pgn, initialFen)
      tags <- pgnDump.tags(game, initialFen, none, withOpening = true, withRatings)
      name <-
        if data.isDefaultName then
          StudyChapterName from Namer.gameVsText(game, withRatings)(using lightUser.async)
        else fuccess(data.name)
      _ = notifyChat(study, game, userId)
    yield Chapter.make(
      studyId = study.id,
      name = name,
      setup = Chapter.Setup(
        !game.synthetic option game.id,
        game.variant,
        data.orientation match
          case Orientation.Auto         => Color.white
          case Orientation.Fixed(color) => color
      ),
      root = root,
      tags = PgnTags(tags),
      order = order,
      ownerId = userId,
      practice = data.isPractice,
      gamebook = data.isGamebook,
      conceal = data.isConceal option root.ply
    )

  def notifyChat(study: Study, game: Game, userId: UserId) =
    if study.isPublic then
      List(game hasUserId userId option game.id.value, s"${game.id}/w".some).flatten foreach { chatId =>
        chatApi.userChat.write(
          chatId = ChatId(chatId),
          userId = userId,
          text = s"I'm studying this game on ${net.domain}/study/${study.id}",
          publicSource = none,
          _.Round,
          persist = false
        )
      }

  private[study] def makeRoot(
      game: Game,
      pgnOpt: Option[PgnStr],
      initialFen: Option[Fen.Epd]
  ): Fu[Root] =
    initialFen.fold(gameRepo initialFen game) { fen =>
      fuccess(fen.some)
    } map { goodFen =>
      val fromGame = GameToRoot(game, goodFen, withClocks = true)
      pgnOpt.flatMap(PgnImport(_, Nil).toOption.map(_.root)) match
        case Some(r) => fromGame.merge(r)
        case None    => fromGame
    }

  private val UrlRegex = {
    val escapedDomain = net.domain.value.replace(".", "\\.")
    s"""$escapedDomain/(\\w{8,12})"""
  }.r.unanchored

  @scala.annotation.tailrec
  private def parseGame(str: String): Fu[Option[Game]] =
    str match
      case s if s.lengthIs == GameId.size     => gameRepo game GameId(s)
      case s if s.lengthIs == GameFullId.size => gameRepo game GameId.take(s)
      case UrlRegex(id)                       => parseGame(id)
      case _                                  => fuccess(none)

private[study] object ChapterMaker:

  case class ValidationException(message: String) extends lila.base.LilaException

  enum Mode:
    def key = toString.toLowerCase
    case Normal, Practice, Gamebook, Conceal
  object Mode:
    def apply(key: String) = values.find(_.key == key)

  trait ChapterData:
    def orientation: Orientation
    def mode: ChapterMaker.Mode
    def isPractice = mode == Mode.Practice
    def isGamebook = mode == Mode.Gamebook
    def isConceal  = mode == Mode.Conceal

  enum Orientation(val key: String, val resolve: Option[Color]):
    case Fixed(color: Color) extends Orientation(color.name, color.some)
    case Auto                extends Orientation("automatic", none)
  object Orientation:
    def apply(str: String) = Color.fromName(str).fold[Orientation](Auto)(Fixed.apply)

  case class Data(
      name: StudyChapterName,
      game: Option[String] = None,
      variant: Option[Variant] = None,
      fen: Option[Fen.Epd] = None,
      pgn: Option[PgnStr] = None,
      orientation: Orientation = Orientation.Auto,
      mode: ChapterMaker.Mode = ChapterMaker.Mode.Normal,
      initial: Boolean = false,
      isDefaultName: Boolean = true
  ) extends ChapterData:

    def manyGames =
      game
        .so(_.linesIterator.take(Study.maxChapters).toList)
        .map(_.trim)
        .filter(_.nonEmpty)
        .map { g => copy(game = g.some) }
        .some
        .filter(_.sizeIs > 1)

  case class EditData(
      id: StudyChapterId,
      name: StudyChapterName,
      orientation: Orientation,
      mode: ChapterMaker.Mode,
      description: String // boolean
  ) extends ChapterData:
    def hasDescription = description.nonEmpty

  case class DescData(id: StudyChapterId, desc: String):
    lazy val clean = lila.common.String.fullCleanUp(desc)
