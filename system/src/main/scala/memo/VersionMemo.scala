package lila.system
package memo

import model._
import lila.chess.{ Color, White, Black }
import scalaz.effects._

final class VersionMemo(
    getPov: (String, Color) ⇒ IO[Pov],
    timeout: Int) {

  private val cache = Builder.cache(timeout, compute)

  def get(gameId: String, color: Color): Int = cache get toKey(gameId, color)

  def put(gameId: String, color: Color, version: Int): IO[Unit] = io {
    cache.put(toKey(gameId, color), version)
  }

  def put(game: DbGame): IO[Unit] = for {
    _ ← put(game.id, White, game.player(White).eventStack.lastVersion)
    _ ← put(game.id, Black, game.player(Black).eventStack.lastVersion)
  } yield ()

  private def toKey(gameId: String, color: Color) = gameId + "." + color.letter

  private def compute(key: String): Int = key.split('.').toList match {
    case gameId :: letter :: Nil ⇒
      letter.headOption flatMap Color.apply map { color ⇒
        getPov(gameId, color).catchLeft.unsafePerformIO.fold(
          error ⇒ 0,
          pov ⇒ pov.player.eventStack.lastVersion | 0
        )
      } getOrElse 0
    case _ ⇒ 0
  }
}
