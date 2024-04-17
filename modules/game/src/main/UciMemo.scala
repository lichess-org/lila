package lila.game

import chess.format.UciDump
import com.github.blemale.scaffeine.Cache

import lila.core.game.Game

final class UciMemo(gameRepo: GameRepo)(using Executor) extends lila.core.game.UciMemo:

  type UciVector = Vector[String]

  private val cache: Cache[GameId, UciVector] = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterAccess(5.minutes)
    .build[GameId, UciVector]()

  private val hardLimit = 300

  def add(game: Game, move: chess.MoveOrDrop): Unit =
    add(game, UciDump.move(game.variant, force960Notation = true)(move))

  private def add(game: Game, uciMove: String): Unit =
    val current = ~cache.getIfPresent(game.id)
    cache.put(game.id, current :+ uciMove)

  def set(game: Game, uciMoves: Seq[String]) =
    cache.put(game.id, uciMoves.toVector)

  def get(game: Game): Fu[UciVector] = get(game, hardLimit)
  def get(game: Game, max: Int): Fu[UciVector] =
    cache
      .getIfPresent(game.id)
      .filter(_.size.atMost(max) == game.sans.size.atMost(max))
      .match
        case Some(moves) => fuccess(moves)
        case _           => compute(game, max).addEffect(set(game, _))

  def drop(game: Game, nb: Int) =
    val current = ~cache.getIfPresent(game.id)
    cache.put(game.id, current.take(current.size - nb))

  def sign(game: Game): Fu[String] = get(game).map { uci =>
    uci.takeRight(5).mkString(" ").takeRight(20).replace(" ", "")
  }

  private def compute(game: Game, max: Int): Fu[UciVector] =
    for
      fen      <- gameRepo.initialFen(game)
      uciMoves <- UciDump(game.sans.take(max), fen, game.variant, force960Notation = true).toFuture
    yield uciMoves.toVector
