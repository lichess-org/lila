package lila.core
package perfStat

import lila.core.perf.PerfType

trait PerfStatApi:
  def highestRating(user: UserId, perfType: PerfType): Fu[Option[IntRating]]
