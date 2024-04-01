package lila.team

export lila.Core.{ *, given }

private val logger = lila.log("team")

type GameTeams = chess.ByColor[TeamId]
