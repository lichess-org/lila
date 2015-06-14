package lila.tournament

case class MiniStanding(
  tour: Tournament,
  standing: Option[RankedPlayers])

case class PlayerInfo(
  rank: Int,
  withdraw: Boolean)
