package lila.user

final class EloUpdater(floor: Int) {

  def game(user: User, elo: Int, opponentElo: Int): Funit = math.max(elo, floor) |> { newElo ⇒
    UserRepo.setElo(user.id, newElo) >> HistoryRepo.addEntry(user.id, newElo, opponentElo.some)
  }

  private def adjustTo = User.STARTING_ELO

  def adjust(u: User) =
    UserRepo.setElo(u.id, adjustTo) >> {
      (!u.engine && u.elo > adjustTo) ?? HistoryRepo.addEntry(u.id, adjustTo, none)
    }
}
