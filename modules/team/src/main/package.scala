package lila.team

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val logger = lila.log("team")

type GameTeams = chess.ByColor[TeamId]
