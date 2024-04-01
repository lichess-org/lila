package lila.user

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val logger = lila.log("user")

type GameUser  = Option[User.WithPerf]
type GameUsers = chess.ByColor[GameUser]
