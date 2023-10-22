package lila.tutor

import lila.rating.PerfType

case class TutorPeriodReport(
    user: UserId,
    at: Instant,
    nbGames: TutorPeriodReport.NbGames,
    report: TutorPerfReport
)

object TutorPeriodReport:

  opaque type NbGames = Int
  object NbGames extends OpaqueInt[NbGames]:
    val presets: List[NbGames]        = List(10_000, 2_000, 500, 100, 20)
    def from(i: Int): Option[NbGames] = presets.find(_ == i)

  case class Query(user: UserId, perf: PerfType, nb: NbGames)

  object F:
    val user       = "user"
    val at         = "at"
    val nbGames    = "nbGames"
    val report     = "report"
    val perf       = s"$report.perf"
    val meanRating = s"$report.stats.rating"
    val millis     = "millis"
