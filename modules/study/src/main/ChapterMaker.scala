package lila.study

import lila.game.{ Game, Pov, GameRepo }
import lila.user.User

private final class ChapterMaker(domain: String) {

  def apply(study: Study, data: Chapter.FormData, order: Int): Fu[Option[Chapter]] = {
    parsePov(data.game) flatMap {
      case None => fuccess {
        Chapter.make(
          studyId = study.id,
          name = data.name,
          setup = Chapter.Setup(none, chess.variant.Standard, chess.White),
          root = Node.Root.default,
          order = order).some
      }
      case Some(pov) => game2root(pov.game, study.ownerId) map { root =>
        Chapter.make(
          studyId = study.id,
          name = data.name,
          setup = Chapter.Setup(pov.game.id.some, pov.game.variant, pov.color),
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
