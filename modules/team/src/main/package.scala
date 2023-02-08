package lila.team

export lila.Lila.{ *, given }

private val logger = lila.log("team")

type GameTeams = chess.Color.Map[TeamId]

case class InsertTeam(team: Team)
case class RemoveTeam(id: TeamId)

import lila.hub.LightTeam.TeamName

opaque type GetTeamNameSync = TeamId => Option[TeamName]
object GetTeamNameSync extends TotalWrapper[GetTeamNameSync, TeamId => Option[TeamName]]
