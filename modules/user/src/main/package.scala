package lila.user

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*
export lila.rating.UserWithPerfs

private val logger = lila.log("user")

val onlineBotVisible = Max(200)

case class LightCount(user: lila.core.LightUser, count: Int)
