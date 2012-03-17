package lila.system
package memo

import model._
import lila.chess.{ Color, White, Black }
import scalaz.effects._

final class VersionMemo(repo: GameRepo) {

  private val cache = Builder.cache(1800, compute)

  def get(gameId: String, color: Color): Int = cache get {
    (gameId, color == White)
  }

  def put(gameId: String, color: Color, version: Int): IO[Unit] = io {
    cache.put((gameId, color == White), version)
  }

  def put(game: DbGame): IO[Unit] = for {
    _ ← put(game.id, White, game.player(White).eventStack.lastVersion)
    _ ← put(game.id, Black, game.player(Black).eventStack.lastVersion)
  } yield ()

  private def compute(pair: Pair[String, Boolean]): Int = pair match {
    case (gameId, isWhite) ⇒
      repo.playerOnly(gameId, Color(isWhite)).catchLeft.unsafePerformIO.fold(
        error ⇒ 0,
        player ⇒ player.eventStack.lastVersion | 0
      )
  }
}
