package lila.game

import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import scala.concurrent.duration._

import chess.Division
import chess.variant.Variant

final class Divider {

  private val cache: Cache[Game.ID, Division] = Scaffeine()
    .expireAfterAccess(10 minutes)
    .build[Game.ID, Division]

  def apply(game: Game, initialFen: Option[String]): Division =
    if (!Variant.divisionSensibleVariants(game.variant)) Division.empty
    else cache.get(game.id, _ => {
      val div = chess.Replay.boards(
        moveStrs = game.pgnMoves,
        initialFen = initialFen map chess.format.FEN,
        variant = game.variant
      ).toOption.fold(Division.empty)(chess.Divider.apply)
      div
    })
}
