package lila
package memo

import model.DbGame
import scalaz.effects._

final class FinisherLock(timeout: Int) extends BooleanExpiryMemo(timeout) {

  def isLocked(game: DbGame): Boolean = get(game.id)

  def lock(game: DbGame): IO[Unit] = put(game.id)
}
