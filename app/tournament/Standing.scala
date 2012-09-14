package lila
package tournament

import com.mongodb.casbah.query.Imports._

case class Player(
    username: String,
    nbWin: Int,
    nbLoss: Int,
    winStreak: Int,
    score: Int) {
}

object Standing {

  def of(tour: Tournament): Standing = tour.users.map { user ⇒
    tour.pairings
      .filter(_ contains user)
      .foldLeft(Builder(user))(_ + _.winner)
      .player
  } sortBy (p ⇒ -p.score)

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
      val points = win.fold(newWinSeq * 2, loss.fold(0, 1))
      copy(
        nbWin = nbWin + win.fold(1, 0),
        nbLoss = nbLoss + loss.fold(1, 0),
        score = score + points,
        winSeq = newWinSeq,
        bestWinSeq = math.max(bestWinSeq, newWinSeq),
        prevWin = win)
    }

    def player = Player(username, nbWin, nbLoss, bestWinSeq, score)
  }
}
