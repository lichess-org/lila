package lila
package elo

import user.{ UserRepo, User, HistoryRepo }

import scalaz.effects._
import scala.math.max

final class EloUpdater(
    userRepo: UserRepo,
    historyRepo: HistoryRepo,
    floor: Int) {

  def game(user: User, elo: Int, opponentElo: Int): IO[Unit] = max(elo, floor) |> { newElo ⇒
    userRepo.setElo(user.id, newElo) >>
      historyRepo.addEntry(user.id, newElo, opponentElo.some)
  }

  private val adjustTo = User.STARTING_ELO

  def adjust(u: User) =
    userRepo.setElo(u.id, adjustTo) >>
      historyRepo.addEntry(u.id, adjustTo, none) doIf (!u.engine && u.elo > adjustTo)
}
