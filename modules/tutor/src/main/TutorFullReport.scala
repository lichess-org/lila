package lila.tutor

import lila.rating.PerfType
import lila.user.User
import org.joda.time.DateTime
import chess.Color

case class TutorFullReport(user: User.ID, at: DateTime, perfs: TutorFullReport.PerfMap) {

  def isFresh = at isAfter DateTime.now.minusDays(1)

  def postProcess = copy(perfs = perfs.view.mapValues(_.postProcess).toMap)
}

case class TutorPerfReport(time: TutorTimeReport, openings: TutorOpeningReport.OpeningMap) {

  def nonEmpty = time.games.value > 0 || openings.exists(_.nonEmpty)

  def postProcess = copy(openings = openings.map(TutorOpeningReport.postProcess))
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

  def aggregate(report: TutorFullReport, pov: RichPov) =
    if (pov.perfType != PerfType.Blitz) report
    else
      report.copy(
        perfs = report.perfs
          .updatedWith(pov.perfType) { opt =>
            val pr = opt | TutorPerfReport(TutorTimeReport.empty, Color.Map(Map.empty, Map.empty))
            TutorPerfReport(
              time = TutorTimeReport.aggregate(pr.time, pov),
              openings = TutorOpeningReport.aggregate(pr.openings, pov)
            ).some
          }
      )
}
