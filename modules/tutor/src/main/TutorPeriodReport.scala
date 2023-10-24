package lila.tutor

import reactivemongo.api.bson.Macros.Annotations.Key
import ornicar.scalalib.ThreadLocalRandom
import lila.rating.PerfType
import lila.user.User

case class TutorPeriodReport(
    @Key("_id") id: TutorPeriodReport.Id,
    user: UserId,
    at: Instant,
    nbGames: TutorPeriodReport.NbGames,
    report: TutorPerfReport
):
  export report.*

object TutorPeriodReport:

  opaque type Id = String
  object Id extends OpaqueString[Id]:
    def make: Id = ThreadLocalRandom nextString 6

  opaque type NbGames = Int
  object NbGames extends OpaqueInt[NbGames]:
    val presets: List[NbGames]        = List(10_000, 2_000, 500, 100, 20)
    def from(i: Int): Option[NbGames] = presets.find(_ == i)

  case class Query(user: UserId, perf: PerfType, nb: NbGames, reportId: TutorPeriodReport.Id):
    override def toString = s"$user ${perf.key} $nb"

  case class Preview(@Key("_id") id: Id, perf: PerfType, nbGames: NbGames, at: Instant)

  case class UserReports(user: User, past: List[Preview], next: Option[TutorQueue.InQueue])

  object F:
    val user       = "user"
    val at         = "at"
    val nbGames    = "nbGames"
    val report     = "report"
    val perf       = s"$report.perf"
    val meanRating = s"$report.stats.rating"
    val millis     = "millis"
