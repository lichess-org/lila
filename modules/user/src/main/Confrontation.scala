package lila.user

case class Confrontation(wins: Int, draws: Int, losses: Int) {

  def games = wins + draws + losses

  def empty = games == 0

  def nonEmpty = !empty
}
