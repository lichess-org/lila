package lila
package tournament

case class Player(
    username: String,
    rank: Int,
    nbWin: Int,
    nbLoss: Int,
    winStreak: Int) {

  def winScore = (nbWin * 2) + winStreak - nbLoss

  def withRank(r: Int) = copy(rank = r)
}

object Player {

  def of(tour: Tournament): List[Player] = {
    val players = tour.users.map { user ⇒
      tour.pairings.filter(_ contains user).foldLeft(Player(user, 0, 0, 0, 0) -> false) {
        case ((player, wonPrev), pairing) ⇒ {
          val won = pairing.winner.fold(_ == user, false)
          val lost = pairing.winner.fold(_ != user, false)
          player.copy(
            nbWin = player.nbWin + won.fold(0, 1),
            nbLoss = player.nbLoss + lost.fold(0, 1),
            winStreak = won.fold(wonPrev.fold(player.winStreak + 1, 1), 0)
          ) -> won
        }
      }._1
    }
    (1 to players.size) zip players.sortBy(s ⇒ -s.winScore) map {
      case (rank, player) ⇒ player withRank rank
    } toList
  }
}
