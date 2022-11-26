package lila.game

import com.github.blemale.scaffeine.Cache
import scala.concurrent.duration.*

import chess.Division
import chess.variant.Variant
import chess.format.FEN

final class Divider:

  private val cache: Cache[GameId, Division] = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterAccess(5 minutes)
    .build[GameId, Division]()

  def apply(game: Game, initialFen: Option[FEN]): Division =
    apply(game.id, game.pgnMoves, game.variant, initialFen)

  def apply(id: GameId, pgnMoves: => PgnMoves, variant: Variant, initialFen: Option[FEN]) =
    if (!Variant.divisionSensibleVariants(variant)) Division.empty
    else cache.get(id, _ => noCache(id, pgnMoves, variant, initialFen))

  def noCache(id: GameId, pgnMoves: => PgnMoves, variant: Variant, initialFen: Option[FEN]) =
    chess.Replay
      .boards(
        moveStrs = pgnMoves,
        initialFen = initialFen,
        variant = variant
      )
      .toOption
      .fold(Division.empty)(chess.Divider.apply)
