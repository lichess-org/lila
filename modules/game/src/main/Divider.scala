package lila.game

import lila.memo.AsyncCache

object Divider {

  type Division = (Option[Int], Option[Int])

  def apply(game: Game) = cache(game)

  private val cache = AsyncCache(generate, maxCapacity = 5000)

  private def generate(game: Game): Fu[Division] =
    GameRepo initialFen game.id map { initialFen =>
      chess.Replay(
        pgn = game.pgnMoves mkString " ",
        initialFen = initialFen,
        variant = game.variant
      ).toOption.fold(none[Int] -> none[Int])(chess.Divider.apply)
    }
}

