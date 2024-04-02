package lila.game

import chess.Division
import chess.format.Fen
import chess.format.pgn.SanStr
import chess.variant.Variant
import com.github.blemale.scaffeine.Cache

final class Divider(using Executor):

  private val cache: Cache[GameId, Division] = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterAccess(5 minutes)
    .build[GameId, Division]()

  def apply(game: Game, initialFen: Option[Fen.Full]): Division =
    apply(game.id, game.sans, game.variant, initialFen)

  def apply(id: GameId, sans: => Vector[SanStr], variant: Variant, initialFen: Option[Fen.Full]) =
    if !Variant.list.divisionSensibleVariants(variant) then Division.empty
    else cache.get(id, _ => noCache(sans, variant, initialFen))

  def noCache(sans: Vector[SanStr], variant: Variant, initialFen: Option[Fen.Full]) =
    chess.Replay
      .boards(
        sans = sans,
        initialFen = initialFen,
        variant = variant
      )
      .fold(_ => Division.empty, chess.Divider.apply)
