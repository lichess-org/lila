package lila
package tournament

case class Standing(players: List[Standing.Player]) {

  def ranked = (1 to players.size) zip players
}

object Standing {

  case class Player(
      username: String,
      nbWin: Int,
      nbLoss: Int,
      winStreak: Int,
      score: Int) {
  }

  def of(tour: Tournament): Standing = Standing {
    tour.users.map { user ⇒
      tour.pairings
        .filter(_ contains user)
        .foldLeft(Builder(user))(_ + _.winner)
        .player
    } sortBy (p ⇒ -p.score)
  }

  private case class Builder(
      username: String,
      nbWin: Int = 0,
      nbLoss: Int = 0,
      score: Int = 0,
      winSeq: Int = 0,
      bestWinSeq: Int = 0,
      prevWin: Boolean = false) {

    def +(winner: Option[String]) = {
      val (win, loss) = winner.fold(
        w ⇒ if (w == username) true -> false else false -> true,
        false -> false)
      val newWinSeq = if (win) prevWin.fold(winSeq + 1, 1) else 0
      copy(
        nbWin = nbWin + win.fold(1, 0),
        nbLoss = nbLoss + loss.fold(1, 0),
        score = score + win.fold(newWinSeq, loss.fold(-1, 0)),
        winSeq = newWinSeq,
        bestWinSeq = math.max(bestWinSeq, newWinSeq),
        prevWin = win)
    }

    def player = Player(username, nbWin, nbLoss, bestWinSeq, score)
  }
}
