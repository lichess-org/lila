package lila.team

export lila.Lila.{ *, given }

private val logger = lila.log("team")

type GameTeams = chess.Color.Map[Team.ID]
