package lila.tournament

case class MiniStanding(
  tour: Tournament,
  standing: Option[RankedPlayers])

case class PlayerInfo(rank: Int, withdraw: Boolean) {
  def page = {
    math.floor((rank - 1) / 10) + 1
  }.toInt
}

case class VisibleTournaments(
  created: List[Tournament],
  started: List[Tournament],
  finished: List[Tournament])

case class PlayerInfoExt(
  tour: Tournament,
  user: lila.user.User,
  player: Player,
  povs: List[lila.game.Pov])
