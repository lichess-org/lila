package lila.game

import lila.memo.Builder
import chess.variant.Variant
import chess.Division

final class Divider {

  private val cache = Builder.size[String, Division](5000)

  def apply(game: Game, initialFen: Option[String]): Division =
    if (!Variant.divisionSensibleVariants.contains(game.variant)) Division.empty
    else Option(cache getIfPresent game.id) | {
      val div = chess.Replay.boards(
        moveStrs = game.pgnMoves,
        initialFen = initialFen map chess.format.FEN,
        variant = game.variant
      ).toOption.fold(Division.empty)(chess.Divider.apply)
      cache.put(game.id, div)
      div
    }
}
