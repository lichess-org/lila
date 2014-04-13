package lila.tournament

import lila.user.User

private[tournament] case class Player(
    id: String,
    username: String,
    rating: Int,
    withdraw: Boolean = false,
    nbWin: Int = 0,
    nbLoss: Int = 0,
    winStreak: Int = 0,
    score: Int = 0) {

  def active = !withdraw

  def is(userId: String): Boolean = id == userId
  def is(user: User): Boolean = is(user.id)
  def is(other: Player): Boolean = is(other.id)

  def doWithdraw = copy(withdraw = true)
  def unWithdraw = copy(withdraw = false)
}

private[tournament] object Player {

  def make(user: User): Player = new Player(
    id = user.id,
    username = user.username,
    rating = user.rating)

  def refresh(tour: Tournament): Players = tour.players.map { player =>
    tour.pairings
      .filter(p => p.finished && (p contains player.id))
      .foldLeft(Builder(player))(_ + _.winner)
      .toPlayer
  } sortBy { p =>
    p.withdraw.fold(Int.MaxValue, 0) - p.score
  }

  private case class Builder(
      player: Player,
      nbWin: Int = 0,
      nbLoss: Int = 0,
      score: Int = 0,
      winSeq: Int = 0,
      bestWinSeq: Int = 0,
      prevWin: Boolean = false) {

    def +(winner: Option[String]) = {
      val (win, loss): Pair[Boolean, Boolean] = winner.fold(false -> false) { w =>
        if (w == player.id) true -> false else false -> true
      }
      val newWinSeq = if (win) prevWin.fold(winSeq + 1, 1) else 0
      val points = win.fold(1 + newWinSeq, loss.fold(0, 1))
      copy(
        nbWin = nbWin + win.fold(1, 0),
        nbLoss = nbLoss + loss.fold(1, 0),
        score = score + points,
        winSeq = newWinSeq,
        bestWinSeq = math.max(bestWinSeq, newWinSeq),
        prevWin = win)
    }

    def toPlayer = player.copy(
      nbWin = nbWin,
      nbLoss = nbLoss,
      winStreak = bestWinSeq,
      score = score)
  }

  import lila.db.JsTube
  import JsTube.Helpers._
  import play.api.libs.json._

  private def defaults = Json.obj(
    "withdraw" -> false,
    "nbWin" -> 0,
    "nbLoss" -> 0,
    "winStreak" -> 0,
    "score" -> 0)

  private[tournament] val tube = JsTube(
    (__.json update merge(defaults)) andThen Json.reads[Player],
    Json.writes[Player]
  )
}
