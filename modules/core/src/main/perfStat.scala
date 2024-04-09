package lila.core
package perfStat

import lila.core.perf.PerfType
import lila.core.rating.IntRating
import lila.core.userId.UserId

trait PerfStatApi:
  def highestRating(user: UserId, perfType: PerfType): Fu[Option[IntRating]]
