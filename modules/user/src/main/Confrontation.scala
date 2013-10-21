package lila.user

case class Confrontation(
  user1: String,
  user2: String,
  wins: Int,
  draws: Int,
  losses: Int) {

  def games = wins + draws + losses

  def empty = games == 0

  def nonEmpty = !empty

  def pov(user: Option[String]): Confrontation = user.fold(this)(pov)

  def pov(user: String): Confrontation = if (user == user1) this else copy(
    user1 = user2,
    user2 = user1,
    wins = losses,
    losses = wins)
}
