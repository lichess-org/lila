package lila.study

import chess.Color
import chess.format.{ Forsyth, FEN }
import chess.variant.Variant
import lila.game.{ Game, Pov, GameRepo }
import lila.importer.{ Importer, ImportData }
import lila.user.User

private final class ChapterMaker(domain: String, importer: Importer) {

  import ChapterMaker._

  def apply(study: Study, data: Data, order: Int): Fu[Option[Chapter]] = {
    val orientation = chess.Color(data.orientation) | chess.White
    data.game.??(parsePov) flatMap {
      case None => data.pgn match {
        case Some(pgn) => fromPgn(study, pgn, data, orientation, order)
        case None      => fuccess(fromFenOrBlank(study, data, orientation, order))
      }
      case Some(pov) => fromPov(study, pov, data, order)
    }
  }

  private def fromPgn(study: Study, pgn: String, data: Data, orientation: Color, order: Int): Fu[Option[Chapter]] =
    importer.inMemory(ImportData(pgn, analyse = none)).fold(
      err => fufail(err.shows),
      game => fromPov(study, Pov(game, orientation), data, order)
    )

  private def fromFenOrBlank(study: Study, data: Data, orientation: Color, order: Int): Option[Chapter] = {
    val variant = data.variant.flatMap(Variant.apply) | Variant.default
    val root = data.fen.map(_.trim).filter(_.nonEmpty).flatMap { fenStr =>
      Forsyth.<<<@(variant, fenStr)
    } match {
      case Some(sit) => Node.Root(
        ply = sit.turns,
        fen = FEN(Forsyth.>>(sit)),
        check = sit.situation.check,
        children = Node.emptyChildren)
      case None => Node.Root(
        ply = 0,
        fen = FEN(variant.initialFen),
        check = false,
        children = Node.emptyChildren)
    }
    Chapter.make(
      studyId = study.id,
      name = data.name,
      setup = Chapter.Setup(none, variant, orientation),
      root = root,
      order = order).some
  }

  private def fromPov(study: Study, pov: Pov, data: Data, order: Int): Fu[Option[Chapter]] =
    game2root(pov.game, study.ownerId) map { root =>
      Chapter.make(
        studyId = study.id,
        name = data.name,
        setup = Chapter.Setup(pov.game.id.some, pov.game.variant, pov.color),
        root = root,
        order = order).some
    }

  private def game2root(game: Game, userId: User.ID): Fu[Node.Root] =
    GameRepo.initialFen(game) map { initialFen =>
      Node.Root.fromRootBy(userId) {
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
    orientation: String)
}
