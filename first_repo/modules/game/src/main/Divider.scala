package lila.game

import com.github.blemale.scaffeine.Cache
import scala.concurrent.duration._

import chess.Division
import chess.variant.Variant
import chess.format.FEN

final class Divider {

  private val cache: Cache[Game.ID, Division] = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterAccess(5 minutes)
    .build[Game.ID, Division]()

  def apply(game: Game, initialFen: Option[FEN]): Division =
    apply(game.id, game.pgnMoves, game.variant, initialFen)

  def apply(id: Game.ID, pgnMoves: => PgnMoves, variant: Variant, initialFen: Option[FEN]) =
    if (!Variant.divisionSensibleVariants(variant)) Division.empty
    else
      cache.get(
        id,
        _ =>
          chess.Replay
            .boards(
              moveStrs = pgnMoves,
              initialFen = initialFen,
              variant = variant
            )
            .toOption
            .fold(Division.empty)(chess.Divider.apply)
      )
}
