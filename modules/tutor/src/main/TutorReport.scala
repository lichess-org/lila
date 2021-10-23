package lila.tutor

import lila.rating.PerfType
import lila.user.User
import org.joda.time.DateTime

case class TutorReport(user: User.ID, createdAt: DateTime, perfMap: Map[PerfType, TutorTimeReport])

object TutorReport {
  val perfTypes = List(
    PerfType.Bullet,
    PerfType.Blitz,
    PerfType.Rapid,
    PerfType.Classical,
    PerfType.Correspondence
  )
  val perfTypeSet: Set[PerfType] = perfTypes.toSet
  def aggregate(report: TutorReport, richPov: RichPov) =
    richPov.pov.game.perfType.filter(perfTypeSet.contains).fold(report) { perfType =>
      report.copy(
        perfMap = report.perfMap
          .updatedWith(perfType)(pt => TutorTimeReport.aggregate(pt | TutorTimeReport.empty, richPov).some)
      )
    }
}
