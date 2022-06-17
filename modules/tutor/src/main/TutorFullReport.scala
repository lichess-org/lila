package lila.tutor

import lila.rating.PerfType
import lila.user.User
import org.joda.time.DateTime
import chess.Color

case class TutorFullReport(user: User.ID, at: DateTime, openings: TutorOpenings) {

  def isFresh = at isAfter DateTime.now.minusDays(1)
}

object TutorFullReport {

  val perfTypes = List(
    PerfType.Bullet,
    PerfType.Blitz,
    PerfType.Rapid,
    PerfType.Classical,
    PerfType.Correspondence
  )
  val perfTypeSet: Set[PerfType] = perfTypes.toSet
}
