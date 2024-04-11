package lila.core
package perf

import lila.core.userId.UserId
import lila.core.rating.data.IntRating

opaque type PerfKey = String
object PerfKey extends OpaqueString[PerfKey]

opaque type PerfId = Int
object PerfId extends OpaqueInt[PerfId]

trait PerfStatApi:
  def highestRating(user: UserId, perfKey: PerfKey): Fu[Option[IntRating]]
