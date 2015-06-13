package lila.tournament

case class MiniStanding(
  tour: Tournament,
  standing: Option[RankedPlayers])

case class VisibleTournaments(
  created: List[Tournament],
  started: List[Tournament],
  finished: List[Tournament])
