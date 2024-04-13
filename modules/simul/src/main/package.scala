package lila.simul

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val logger = lila.log("simul")

def canBeFeatured(u: User) = u.hasTitle && !u.lameOrTroll
