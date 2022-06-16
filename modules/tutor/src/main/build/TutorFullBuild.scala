package lila.tutor
package build

import lila.rating.PerfType
import lila.user.User
import org.joda.time.DateTime
import chess.Color

case class TutorPerfBuild(time: TutorTimeReport, openings: Color.Map[TutorOpeningBuild.OpeningMap]) {

  def nonEmpty = time.games.value > 0 || openings.exists(_.nonEmpty)

  def toReport = TutorPerfReport(time, TutorOpeningBuild toReport openings)
}

object TutorFullBuild {

  type PerfMap = Map[PerfType, TutorPerfBuild]

  def aggregate(report: PerfMap, pov: RichPov) =
    report
      .updatedWith(pov.perfType) { opt =>
        val pr = opt | TutorPerfBuild(TutorTimeReport.empty, Color.Map(Map.empty, Map.empty))
        TutorPerfBuild(
          time = TutorTimeReport.aggregate(pr.time, pov),
          openings = TutorOpeningBuild.aggregate(pr.openings, pov)
        ).some
      }
}
