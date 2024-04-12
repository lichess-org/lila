package lila.core

import lila.core.userId.UserId
import lila.core.rating.data.IntRating
import lila.core.rating.Glicko
import lila.core.rating.data.IntRatingDiff

object perf:

  opaque type PerfKey = String
  object PerfKey extends OpaqueString[PerfKey]

  opaque type PerfId = Int
  object PerfId extends OpaqueInt[PerfId]

  trait PerfStatApi:
    def highestRating(user: UserId, perfKey: PerfKey): Fu[Option[IntRating]]

  case class Perf(
      glicko: Glicko,
      nb: Int,
      recent: List[IntRating],
      latest: Option[Instant]
  ):
    export glicko.{ intRating, intDeviation, provisional }
    export latest.{ isEmpty, nonEmpty }

    def progress: IntRatingDiff = {
      for
        head <- recent.headOption
        last <- recent.lastOption
      yield IntRatingDiff(head.value - last.value)
    } | IntRatingDiff(0)
