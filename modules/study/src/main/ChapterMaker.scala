package lila.study

import chess.format.{ Forsyth, FEN }
import lila.game.{ Game, Pov, GameRepo }
import lila.user.User

private final class ChapterMaker(domain: String) {

  def apply(study: Study, data: ChapterMaker.Data, order: Int): Fu[Option[Chapter]] = {
    val orientation = chess.Color(data.orientation) | chess.White
    parsePov(data.game) flatMap {
      case None => fuccess {
        val variant = chess.variant.Variant orDefault data.variant
        val root = data.fen.trim.some.filter(_.nonEmpty).flatMap { fenStr =>
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
      case Some(pov) => game2root(pov.game, study.ownerId) map { root =>
        Chapter.make(
          studyId = study.id,
          name = data.name,
          setup = Chapter.Setup(pov.game.id.some, pov.game.variant, orientation),
          root = root,
          order = order).some
      }
    }
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
    game: String,
    variant: String,
    fen: String,
    orientation: String)
}
