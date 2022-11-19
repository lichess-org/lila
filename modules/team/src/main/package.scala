package lila.team

export lila.Lila.{ *, given }

private def logger = lila.log("team")

type GameTeams = chess.Color.Map[Team.ID]
