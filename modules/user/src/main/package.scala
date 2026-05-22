package lila.user

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*
export lila.rating.UserWithPerfs

private val logger = lila.log("user")

case class LightCount(user: lila.core.LightUser, count: Int)
