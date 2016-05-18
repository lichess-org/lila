package lila.study

import chess.Color
import chess.format.{ Forsyth, FEN }
import chess.variant.{ Variant, Crazyhouse }
import lila.game.{ Game, Pov, GameRepo }
import lila.importer.{ Importer, ImportData }
import lila.user.User

private final class ChapterMaker(domain: String, importer: Importer) {

  import ChapterMaker._

  def apply(study: Study, data: Data, order: Int, userId: User.ID): Fu[Option[Chapter]] = {
    val orientation = Color(data.orientation) | chess.White
    data.game.??(parsePov) flatMap {
      case None => data.pgn match {
        case Some(pgn) => fromPgn(study, pgn, data, orientation, order, userId)
        case None      => fuccess(fromFenOrBlank(study, data, orientation, order, userId))
      }
      case Some(pov) => fromPov(study, pov, data, order, userId)
    }
  }

  private def fromPgn(study: Study, pgn: String, data: Data, orientation: Color, order: Int, userId: User.ID): Fu[Option[Chapter]] =
    PgnImport(pgn).future map { res =>
      Chapter.make(
        studyId = study.id,
        name = data.name,
        setup = Chapter.Setup(
          none,
          res.variant,
          orientation,
          fromPgn = Chapter.FromPgn(
            tags = res.tags).some),
        root = res.root,
        order = order,
        ownerId = userId).some
    }

  private def fromFenOrBlank(study: Study, data: Data, orientation: Color, order: Int, userId: User.ID): Option[Chapter] = {
    val variant = data.variant.flatMap(Variant.apply) | Variant.default
    val root = data.fen.map(_.trim).filter(_.nonEmpty).flatMap { fenStr =>
      Forsyth.<<<@(variant, fenStr)
    } match {
      case Some(sit) => Node.Root(
        ply = sit.turns,
        fen = FEN(Forsyth.>>(sit)),
        check = sit.situation.check,
        crazyData = sit.situation.board.crazyData,
        children = Node.emptyChildren)
      case None => Node.Root(
        ply = 0,
        fen = FEN(variant.initialFen),
        check = false,
        crazyData = variant.crazyhouse option Crazyhouse.Data.init,
        children = Node.emptyChildren)
    }
    Chapter.make(
      studyId = study.id,
      name = data.name,
      setup = Chapter.Setup(none, variant, orientation),
      root = root,
      order = order,
      ownerId = userId).some
  }

  private def fromPov(study: Study, pov: Pov, data: Data, order: Int, userId: User.ID, initialFen: Option[FEN] = None): Fu[Option[Chapter]] =
    game2root(pov.game, initialFen) map { root =>
      Chapter.make(
        studyId = study.id,
        name = data.name,
        setup = Chapter.Setup(
          !pov.game.synthetic option pov.game.id,
          pov.game.variant,
          pov.color),
        root = root,
        order = order,
        ownerId = userId).some
    }

  private def game2root(game: Game, initialFen: Option[FEN] = None): Fu[Node.Root] =
    initialFen.fold(GameRepo.initialFen(game)) { fen =>
      fuccess(fen.value.some)
    } map { initialFen =>
      Node.Root.fromRoot {
        lila.round.TreeBuilder(
          game = game,
          a = none,
          initialFen = initialFen | game.variant.initialFen,
          withOpening = false)
      }
    }

  private val UrlRegex = {
    val escapedDomain = domain.replace(".", "\\.")
    s""".*$escapedDomain/(\\w{8,12}).*"""
  }.r

  private def parsePov(str: String): Fu[Option[Pov]] = str match {
    case s if s.size == Game.gameIdSize => GameRepo.pov(s, chess.White)
    case s if s.size == Game.fullIdSize => GameRepo.pov(s)
    case UrlRegex(id)                   => parsePov(id)
    case _                              => fuccess(none)
  }
}

private[study] object ChapterMaker {

  case class Data(
    name: String,
    game: Option[String],
    variant: Option[String],
    fen: Option[String],
    pgn: Option[String],
    orientation: String,
    initial: Boolean)
}
