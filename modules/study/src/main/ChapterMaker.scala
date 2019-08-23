package lidraughts.study

import draughts.format.pdn.Tags
import draughts.format.{ Forsyth, FEN }
import draughts.variant.Variant
import lidraughts.chat.Chat
import lidraughts.game.{ Game, GameRepo, Namer }
import lidraughts.importer.Importer
import lidraughts.user.User

private final class ChapterMaker(
    domain: String,
    lightUser: lidraughts.user.LightUserApi,
    chat: akka.actor.ActorSelection,
    importer: Importer,
    pdnFetch: PdnFetch,
    pdnDump: lidraughts.game.PdnDump
) {

  import ChapterMaker._

  def apply(study: Study, data: Data, order: Int, userId: User.ID, draughtsResult: Boolean): Fu[Chapter] =
    data.game.??(parseGame) flatMap {
      case None =>
        data.game ?? pdnFetch.fromUrl flatMap {
          case Some(pdn) => fromFenOrPdnOrBlank(study, data.copy(pdn = pdn.some), order, userId, draughtsResult)
          case _ => fromFenOrPdnOrBlank(study, data, order, userId, draughtsResult)
        }
      case Some(game) => fromGame(study, game, data, order, userId, none, draughtsResult)
    } map { (c: Chapter) =>
      if (c.name.value.isEmpty) c.copy(name = Chapter defaultName order) else c
    }

  def fromFenOrPdnOrBlank(study: Study, data: Data, order: Int, userId: User.ID, draughtsResult: Boolean): Fu[Chapter] =
    data.pdn.filter(_.trim.nonEmpty) match {
      case Some(pdn) => fromPdn(study, pdn, data, order, userId, draughtsResult)
      case None => fuccess(fromFenOrBlank(study, data, order, userId))
    }

  private def fromPdn(study: Study, pdn: String, data: Data, order: Int, userId: User.ID, draughtsResult: Boolean): Fu[Chapter] = for {
    contributors <- lightUser.asyncMany(study.members.contributorIds.toList)
    parsed <- PdnImport(pdn, contributors.flatten, draughtsResult).future recoverWith {
      case e: Exception => fufail(ValidationException(e.getMessage))
    }
  } yield Chapter.make(
    studyId = study.id,
    name = (for {
      white <- parsed.tags(_.White)
      black <- parsed.tags(_.Black)
      if data.name.value.isEmpty || Chapter.isDefaultName(data.name)
    } yield Chapter.Name(s"$white - $black")) | data.name,
    setup = Chapter.Setup(
      none,
      parsed.variant,
      data.realOrientation
    ),
    root = parsed.root,
    tags = parsed.tags,
    order = order,
    ownerId = userId,
    practice = data.isPractice,
    gamebook = data.isGamebook,
    conceal = data.isConceal option Chapter.Ply(parsed.root.ply)
  )

  private def fromFenOrBlank(study: Study, data: Data, order: Int, userId: User.ID): Chapter = {
    val variant = data.variant.flatMap(Variant.apply) | Variant.default
    (data.fen.map(_.trim).filter(_.nonEmpty).flatMap { fenStr =>
      Forsyth.<<<@(variant, fenStr)
    } match {
      case Some(sit) => Node.Root(
        ply = sit.turns,
        fen = FEN(Forsyth.>>(sit)),
        clock = none,
        children = Node.emptyChildren
      ) -> true
      case None => Node.Root(
        ply = 0,
        fen = FEN(variant.initialFen),
        clock = none,
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
    initialFen: Option[FEN],
    draughtsResult: Boolean
  ): Fu[Chapter] = for {
    root <- game2root(game, initialFen, draughtsResult)
    tags <- pdnDump.tags(game, initialFen, none, withOpening = true, draughtsResult = draughtsResult)
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
    tags = PdnTags(tags),
    order = order,
    ownerId = userId,
    practice = data.isPractice,
    gamebook = data.isGamebook,
    conceal = data.isConceal option Chapter.Ply(root.ply)
  )

  def notifyChat(study: Study, game: Game, userId: User.ID) =
    if (study.isPublic) List(game.id, s"${game.id}/w") foreach { chatId =>
      chat ! lidraughts.chat.actorApi.UserTalk(
        chatId = Chat.Id(chatId),
        userId = userId,
        text = s"I'm studying this game on ${domain}/study/${study.id}",
        publicSource = none
      )
    }

  def game2root(game: Game, initialFen: Option[FEN], draughtsResult: Boolean): Fu[Node.Root] =
    initialFen.fold(GameRepo initialFen game) { fen =>
      fuccess(fen.some)
    } map { GameToRoot(game, _, withClocks = true, draughtsResult = draughtsResult, mergeCapts = true) }

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

  case class ValidationException(message: String) extends lidraughts.base.LidraughtsException

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
    def realOrientation = draughts.Color(orientation) | draughts.White
    def isPractice = mode == Mode.Practice.key
    def isGamebook = mode == Mode.Gamebook.key
    def isConceal = mode == Mode.Conceal.key
  }

  case class Data(
      name: Chapter.Name,
      game: Option[String] = None,
      variant: Option[String] = None,
      fen: Option[String] = None,
      pdn: Option[String] = None,
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
