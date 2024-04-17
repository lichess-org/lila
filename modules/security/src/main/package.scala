package lila.security

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val logger = lila.log("security")

def canFullyLogin(u: User) = u.enabled.yes || !(u.lameOrTroll || u.marks.alt)
