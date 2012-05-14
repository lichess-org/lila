package lila
package user

import scalaz.effects._

final class EloUpdater(userRepo: UserRepo, historyRepo: HistoryRepo) {

  def game(user: User, elo: Int, gameId: String): IO[Unit] =
    userRepo.setElo(user.id, elo) flatMap { _ ⇒
      historyRepo.addEntry(user.usernameCanonical, elo, Some(gameId))
    }

  def adjust(user: User, elo: Int): IO[Unit] =
    userRepo.setElo(user.id, elo) flatMap { _ ⇒
      historyRepo.addEntry(user.usernameCanonical, elo, entryType = HistoryRepo.TYPE_ADJUST)
    }
}
