package lila.user

export lila.Lila.{ *, given }

private val logger = lila.log("user")

type GameUser  = Option[lila.common.LightUser.Ghost | User.WithPerf]
type GameUsers = chess.ByColor[GameUser]
