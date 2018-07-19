package lila.hub

package object tournamentTeam {
  type TeamId = String
  type TeamName = String
  type TeamIdList = List[TeamId]
  type TeamIdsWithNames = List[(TeamId, TeamName)]
}