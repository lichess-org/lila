package lila.core
package perfStat

import lila.core.rating.PerfType

trait PerfStatApi:
  def highestRating(user: UserId, perfType: PerfType): Fu[Option[IntRating]]
