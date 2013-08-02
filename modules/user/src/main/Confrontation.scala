package lila.user

case class Confrontation(
  user1: User,
  user2: User,
  wins: Int,
  draws: Int,
  losses: Int) {

  def games = wins + draws + losses

  def empty = games == 0

  def nonEmpty = !empty
}
