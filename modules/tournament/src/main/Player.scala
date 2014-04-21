package lila.tournament

import lila.user.User

private[tournament] case class Player(
    id: String,
    username: String,
    rating: Int,
    withdraw: Boolean = false,
    score: Int = 0) {

  def active = !withdraw

  def is(userId: String): Boolean = id == userId
  def is(user: User): Boolean = is(user.id)
  def is(other: Player): Boolean = is(other.id)

  def doWithdraw = copy(withdraw = true)
  def unWithdraw = copy(withdraw = false)
}

private[tournament] object Player {

  private[tournament] def make(user: User): Player = new Player(
    id = user.id,
    username = user.username,
    rating = user.rating)

  private[tournament] def refresh(tour: Tournament): Players = tour.players map { p =>
    p.copy(score = Score.sheet(p.id, tour).total)
  } sortBy { p =>
    p.withdraw.fold(Int.MaxValue, 0) - p.score
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
