package lila
package user

import scalaz.effects._
import scala.math.max

final class EloUpdater(
  userRepo: UserRepo, 
  historyRepo: HistoryRepo,
  floor: Int) {

  def game(user: User, elo: Int, gameId: String): IO[Unit] = {
    val newElo = max(elo, floor)
    userRepo.setElo(user.id, newElo) flatMap { _ ⇒
      historyRepo.addEntry(user.id, newElo, Some(gameId))
    }
  }

  def adjust(user: User, elo: Int): IO[Unit] =
    userRepo.setElo(user.id, elo) flatMap { _ ⇒
      historyRepo.addEntry(user.id, elo, entryType = HistoryRepo.TYPE_ADJUST)
    }
}
