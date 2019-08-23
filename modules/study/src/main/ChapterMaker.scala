package lila.study

import chess.format.pgn.Tags
import chess.format.{ Forsyth, FEN }
import chess.variant.{ Variant, Crazyhouse }
import lila.chat.Chat
import lila.game.{ Game, GameRepo, Namer }
import lila.importer.Importer
import lila.user.User

private final class ChapterMaker(
    domain: String,
    lightUser: lila.user.LightUserApi,
    chat: akka.actor.ActorSelection,
    importer: Importer,
    pgnFetch: PgnFetch,
    pgnDump: lila.game.PgnDump
) {

  import ChapterMaker._

  def apply(study: Study, data: Data, order: Int, userId: User.ID): Fu[Chapter] =
    data.game.??(parseGame) flatMap {
      case None =>
        data.game.??(pgnFetch.fromUrl) flatMap {
          case Some(pgn) => fromFenOrPgnOrBlank(study, data.copy(pgn = pgn.some), order, userId)
          case _ => fromFenOrPgnOrBlank(study, data, order, userId)
        }
      case Some(game) => fromGame(study, game, data, order, userId)
    } map { (c: Chapter) =>
      if (c.name.value.isEmpty) c.copy(name = Chapter defaultName order) else c
    }

  def fromFenOrPgnOrBlank(study: Study, data: Data, order: Int, userId: User.ID): Fu[Chapter] =
    data.pgn.filter(_.trim.nonEmpty) match {
      case Some(pgn) => fromPgn(study, pgn, data, order, userId)
      case None => fuccess(fromFenOrBlank(study, data, order, userId))
    }

  private def fromPgn(study: Study, pgn: String, data: Data, order: Int, userId: User.ID): Fu[Chapter] =
    lightUser.asyncMany(study.members.contributorIds.toList) map { contributors =>
      PgnImport(pgn, contributors.flatten).toOption.fold(fromFenOrBlank(study, data, order, userId)) { res =>
        Chapter.make(
          studyId = study.id,
          name = (for {
            white <- res.tags(_.White)
            black <- res.tags(_.Black)
            if data.name.value.isEmpty || Chapter.isDefaultName(data.name)
          } yield Chapter.Name(s"$white - $black")) | data.name,
          setup = Chapter.Setup(
            none,
            res.variant,
            data.realOrientation
          ),
          root = res.root,
          tags = res.tags,
          order = order,
          ownerId = userId,
          practice = data.isPractice,
          gamebook = data.isGamebook,
          conceal = data.isConceal option Chapter.Ply(res.root.ply)
        )
      }
    }

  private def fromFenOrBlank(study: Study, data: Data, order: Int, userId: User.ID): Chapter = {
    val variant = data.variant.flatMap(Variant.apply) | Variant.default
    (data.fen.map(_.trim).filter(_.nonEmpty).flatMap { fenStr =>
      Forsyth.<<<@(variant, fenStr)
    } match {
      case Some(sit) => Node.Root(
        ply = sit.turns,
        fen = FEN(Forsyth.>>(sit)),
        check = sit.situation.check,
        clock = none,
        crazyData = sit.situation.board.crazyData,
        children = Node.emptyChildren
      ) -> true
      case None => Node.Root(
        ply = 0,
        fen = FEN(variant.initialFen),
        check = false,
        clock = none,
        crazyData = variant.crazyhouse option Crazyhouse.Data.init,
        children = Node.emptyChildren
      ) -> false
    }) match {
      case (root, isFromFen) => Chapter.make(
        studyId = study.id,
        name = data.name,
        setup = Chapter.Setup(
          none,
          variant,
          data.realOrientation,
          fromFen = isFromFen option true
        ),
        root = root,
        tags = Tags.empty,
        order = order,
        ownerId = userId,
        practice = data.isPractice,
        gamebook = data.isGamebook,
        conceal = None
      )
    }
  }

  private def fromGame(
    study: Study,
    game: Game,
    data: Data,
    order: Int,
    userId: User.ID,
    initialFen: Option[FEN] = None
  ): Fu[Chapter] = for {
    root <- game2root(game, initialFen)
    tags <- pgnDump.tags(game, initialFen, none, withOpening = true)
    _ = notifyChat(study, game, userId)
  } yield Chapter.make(
    studyId = study.id,
    name =
      if (Chapter isDefaultName data.name)
        Chapter.Name(Namer.gameVsText(game, withRatings = false)(lightUser.sync))
      else data.name,
    setup = Chapter.Setup(
      !game.synthetic option game.id,
      game.variant,
      data.realOrientation
    ),
    root = root,
    tags = PgnTags(tags),
    order = order,
    ownerId = userId,
    practice = data.isPractice,
    gamebook = data.isGamebook,
    conceal = data.isConceal option Chapter.Ply(root.ply)
  )

  def notifyChat(study: Study, game: Game, userId: User.ID) =
    if (study.isPublic) List(game.id, s"${game.id}/w") foreach { chatId =>
      chat ! lila.chat.actorApi.UserTalk(
        chatId = Chat.Id(chatId),
        userId = userId,
        text = s"I'm studying this game on ${domain}/study/${study.id}",
        publicSource = none
      )
    }

  def game2root(game: Game, initialFen: Option[FEN] = None): Fu[Node.Root] =
    initialFen.fold(GameRepo initialFen game) { fen =>
      fuccess(fen.some)
    } map { GameToRoot(game, _, withClocks = true) }

  private val UrlRegex = {
    val escapedDomain = domain.replace(".", "\\.")
    s"""$escapedDomain/(\\w{8,12})"""
  }.r.unanchored

  private def parseGame(str: String): Fu[Option[Game]] = str match {
    case s if s.size == Game.gameIdSize => GameRepo game s
    case s if s.size == Game.fullIdSize => GameRepo game Game.takeGameId(s)
    case UrlRegex(id) => parseGame(id)
    case _ => fuccess(none)
  }
}

private[study] object ChapterMaker {

  sealed trait Mode {
    def key = toString.toLowerCase
  }
  object Mode {
    case object Normal extends Mode
    case object Practice extends Mode
    case object Gamebook extends Mode
    case object Conceal extends Mode
    val all = List(Normal, Practice, Gamebook, Conceal)
    def apply(key: String) = all.find(_.key == key)
  }

  trait ChapterData {
    def orientation: String
    def mode: String
    def realOrientation = chess.Color(orientation) | chess.White
    def isPractice = mode == Mode.Practice.key
    def isGamebook = mode == Mode.Gamebook.key
    def isConceal = mode == Mode.Conceal.key
  }

  case class Data(
      name: Chapter.Name,
      game: Option[String] = None,
      variant: Option[String] = None,
      fen: Option[String] = None,
      pgn: Option[String] = None,
      orientation: String = "white",
      mode: String = ChapterMaker.Mode.Normal.key,
      initial: Boolean = false
  ) extends ChapterData

  case class EditData(
      id: Chapter.Id,
      name: Chapter.Name,
      orientation: String,
      mode: String,
      description: String // boolean
  ) extends ChapterData {
    def hasDescription = description.nonEmpty
  }

  case class DescData(id: Chapter.Id, desc: String)
}
