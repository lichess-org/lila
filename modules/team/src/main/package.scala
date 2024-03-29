package lila.team

export lila.Lila.{ *, given }

private val logger = lila.log("team")

type GameTeams = chess.ByColor[TeamId]

case class InsertTeam(team: Team)
case class RemoveTeam(id: TeamId)
