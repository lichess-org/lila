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
}
