package lila.plan

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*
export lila.core.user.Plan.*

private val logger = lila.log("plan")

import lila.core.user.Plan
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
