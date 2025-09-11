package lila.user

import lila.core.user.Plan

object Plan:

  extension (p: Plan) def sinceDate = p.since | nowInstant

  val empty = new Plan(0, active = false, lifetime = false, since = none)
