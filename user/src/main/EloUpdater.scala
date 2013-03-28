package lila.user

import scala.math.max

final class EloUpdater(floor: Int) {

  def game(user: User, elo: Int, opponentElo: Int): Funit = max(elo, floor) |> { newElo ⇒
    UserRepo.setElo(user.id, newElo) >> HistoryRepo.addEntry(user.id, newElo, opponentElo.some)
  }

  private def adjustTo = Users.STARTING_ELO

  def adjust(u: User) =
    UserRepo.setElo(u.id, adjustTo) >>
      HistoryRepo.addEntry(u.id, adjustTo, none) doIf (!u.engine && u.elo > adjustTo)
}
