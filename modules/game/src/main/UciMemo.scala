package lila.game

import com.github.blemale.scaffeine.Cache
import scala.concurrent.duration._

import shogi.format.UsiDump

final class UsiMemo(gameRepo: GameRepo)(implicit ec: scala.concurrent.ExecutionContext) {

  type UsiVector = Vector[String]

  private val cache: Cache[Game.ID, UsiVector] = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterAccess(5 minutes)
    .build[Game.ID, UsiVector]()

  private val hardLimit = 300

  def add(game: Game, usiMove: String): Unit = {
    val current = ~cache.getIfPresent(game.id)
    cache.put(game.id, current :+ usiMove)
  }
  def add(game: Game, move: shogi.MoveOrDrop): Unit =
    add(game, UsiDump.move(move))

  def set(game: Game, usiMoves: Seq[String]) =
    cache.put(game.id, usiMoves.toVector)

  def get(game: Game, max: Int = hardLimit): Fu[UsiVector] =
    cache getIfPresent game.id filter { moves =>
      moves.size.min(max) == game.pgnMoves.size.min(max)
    } match {
      case Some(moves) => fuccess(moves)
      case _           => compute(game, max) addEffect { set(game, _) }
    }

  def drop(game: Game, nb: Int) = {
    val current = ~cache.getIfPresent(game.id)
    cache.put(game.id, current.take(current.size - nb))
  }

  private def compute(game: Game, max: Int): Fu[UsiVector] =
    for {
      fen      <- gameRepo initialFen game
      usiMoves <- UsiDump(game.pgnMoves.take(max), fen.map(_.value), game.variant).toFuture
    } yield usiMoves.toVector
}
