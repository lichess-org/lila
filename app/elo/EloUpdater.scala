package lila
package elo

import user.{ UserRepo, User, HistoryRepo }

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

  val adjustTo = User.STARTING_ELO

  def adjust(u: User) = for {
    _ ← userRepo toggleEngine u.id
    _ ← (!u.engine && u.elo > adjustTo).fold(
      userRepo.setElo(u.id, adjustTo) flatMap { _ ⇒
        historyRepo.addEntry(u.id, adjustTo, entryType = HistoryRepo.TYPE_ADJUST)
      },
      io())
  } yield ()
}
