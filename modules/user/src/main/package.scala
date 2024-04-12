package lila.user

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*
export lila.rating.UserWithPerfs

private val logger = lila.log("user")

type GameUser  = Option[lila.core.user.WithPerf]
type GameUsers = chess.ByColor[GameUser]
