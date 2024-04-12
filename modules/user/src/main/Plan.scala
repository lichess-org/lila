package lila.user

import lila.core.user.Plan

object Plan:

  extension (p: Plan)

    def incMonths: Plan =
      p.copy(
        months = p.months + 1,
        active = true,
        since = p.since.orElse(nowInstant.some)
      )

    def disable: Plan = p.copy(active = false)

    def enable: Plan =
      p.copy(
        active = true,
        months = p.months.atLeast(1),
        since = p.since.orElse(nowInstant.some)
      )

    def isEmpty: Boolean = p.months == 0

    def nonEmpty: Option[Plan] = Option.when(!p.isEmpty)(p)

    def sinceDate = p.since | nowInstant

  val empty = new Plan(0, active = false, none)
  def start = new Plan(1, active = true, nowInstant.some)
