package lila.pool

export lila.Lila.{ *, given }

private val logger = lila.log("pool")

import alleycats.Zero

opaque type Blocking = Set[UserId]
object Blocking extends TotalWrapper[Blocking, Set[UserId]]:
  given Zero[Blocking] = Zero(Set.empty)
