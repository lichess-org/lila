package lila.user

export lila.Lila.{ *, given }

private val logger = lila.log("user")

type GameUser  = Option[User.WithPerf]
type GameUsers = chess.ByColor[GameUser]
