package lila.tournament

case class MiniStanding(
  tour: Tournament,
  standing: Option[RankedPlayers])

case class PlayerInfo(
    rank: Int,
    withdraw: Boolean) {
  def page = {
    math.floor((rank - 1) / 10) + 1
  }.toInt
}
