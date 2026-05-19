package lila.game

import chess.Division
import chess.format.Fen
import chess.format.pgn.SanStr
import chess.variant.Variant
import com.github.blemale.scaffeine.Cache

final class Divider(using Executor) extends lila.core.game.Divider:

  private val cache: Cache[GameId, Division] = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterAccess(5.minutes)
    .build[GameId, Division]()

  def apply(game: CoreGame, initialFen: Option[Fen.Full]): Division =
    apply(game.id, game.sans, game.variant, initialFen)

  def apply(id: GameId, sans: => Vector[SanStr], variant: Variant, initialFen: Option[Fen.Full]): Division =
    if !Variant.list.divisionSensibleVariants(variant) then Division.empty
    else cache.get(id, _ => noCache(sans, variant, initialFen))

  private def noCache(sans: Vector[SanStr], variant: Variant, initialFen: Option[Fen.Full]) =
    chess
      .Position(variant, initialFen)
      .playBoards(sans)
      .fold(_ => Division.empty, chess.Divider.apply)
