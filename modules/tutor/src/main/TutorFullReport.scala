package lila.tutor

import lila.rating.PerfType
import lila.user.User
import org.joda.time.DateTime

case class TutorFullReport(user: User.ID, at: DateTime, perfMap: TutorFullReport.PerfMap) {

  def isFresh = at isAfter DateTime.now.minusDays(1)
}

object TutorFullReport {

  type PerfMap = Map[PerfType, TutorTimeReport]

  val perfTypes = List(
    PerfType.Bullet,
    PerfType.Blitz,
    PerfType.Rapid,
    PerfType.Classical,
    PerfType.Correspondence
  )
  val perfTypeSet: Set[PerfType] = perfTypes.toSet

  def aggregate(report: TutorFullReport, pov: RichPov) =
    report.copy(
      perfMap = report.perfMap
        .updatedWith(pov.perfType)(pt => TutorTimeReport.aggregate(pt | TutorTimeReport.empty, pov).some)
    )
}
