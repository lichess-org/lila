package lila.tutor

import lila.rating.PerfType
import lila.user.User
import org.joda.time.DateTime
import chess.Color

case class TutorFullReport(user: User.ID, at: DateTime, perfs: TutorFullReport.PerfMap) {

  def isFresh = at isAfter DateTime.now.minusDays(1)
}

case class TutorPerfReport(time: TutorTimeReport, openings: TutorOpeningReport.OpeningMap) {

  def games = time.games
}

object TutorFullReport {

  type PerfMap = Map[PerfType, TutorPerfReport]

  val perfTypes = List(
    PerfType.Bullet,
    PerfType.Blitz,
    PerfType.Rapid,
    PerfType.Classical,
    PerfType.Correspondence
  )
  val perfTypeSet: Set[PerfType] = perfTypes.toSet
}
