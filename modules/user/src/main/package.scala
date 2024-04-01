package lila.user

export lila.Core.{ *, given }

private val logger = lila.log("user")

type GameUser  = Option[User.WithPerf]
type GameUsers = chess.ByColor[GameUser]
