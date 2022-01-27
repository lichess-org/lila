package lila.game

import com.github.blemale.scaffeine.Cache
import scala.concurrent.duration._

import chess.format.UciDump

final class UciMemo(gameRepo: GameRepo)(implicit ec: scala.concurrent.ExecutionContext) {

  type UciVector = Vector[String]

  private val cache: Cache[Game.ID, UciVector] = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterAccess(5 minutes)
    .build[Game.ID, UciVector]()

  private val hardLimit = 300

  def add(game: Game, uciMove: String): Unit = {
    val current = ~cache.getIfPresent(game.id)
    cache.put(game.id, current :+ uciMove)
  }
  def add(game: Game, move: chess.MoveOrDrop): Unit =
    add(game, UciDump.move(game.variant, force960Notation = true)(move))

  def set(game: Game, uciMoves: Seq[String]) =
    cache.put(game.id, uciMoves.toVector)

  def get(game: Game, max: Int = hardLimit): Fu[UciVector] =
    cache getIfPresent game.id filter { moves =>
      moves.size.atMost(max) == game.pgnMoves.size.atMost(max)
    } match {
      case Some(moves) => fuccess(moves)
      case _           => compute(game, max) addEffect { set(game, _) }
    }

  def drop(game: Game, nb: Int) = {
    val current = ~cache.getIfPresent(game.id)
    cache.put(game.id, current.take(current.size - nb))
  }

  private def compute(game: Game, max: Int): Fu[UciVector] =
    for {
      fen      <- gameRepo initialFen game
      uciMoves <- UciDump(game.pgnMoves take max, fen, game.variant, force960Notation = true).toFuture
    } yield uciMoves.toVector
}
