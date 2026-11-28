package lila.fishnet

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

lazy val logger = lila.log("fishnet")

opaque type FishnetKey = String
object FishnetKey extends OpaqueString[FishnetKey]
