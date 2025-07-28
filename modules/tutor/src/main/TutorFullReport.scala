package lila.tutor

import lila.rating.PerfType
import lila.tutor.TutorCompare.AnyComparison

case class TutorFullReport(
    user: UserId,
    at: Instant,
    perfs: List[TutorPerfReport]
):
  def apply(perfType: PerfType) = perfs.find(_.perf == perfType)
  def isFresh = at.isAfter(nowInstant.minusMinutes(TutorFullReport.freshness.toMinutes.toInt))

  lazy val nbGames = perfs.toList.map(_.stats.totalNbGames).sum
  lazy val totalTime: FiniteDuration =
    perfs.toList.flatMap(_.estimateTotalTime).foldLeft(0.minutes)(_ + _)

  def favouritePerfs: List[TutorPerfReport] = perfs.headOption.so:
    _ :: perfs.tailSafe.takeWhile: perf =>
      perf.estimateTotalTime.exists(_ > totalTime * 0.25)

  def percentTimeOf(perf: PerfType): Option[GoodPercent] =
    apply(perf).flatMap(_.estimateTotalTime).map { time =>
      GoodPercent(time.toSeconds.toDouble, totalTime.toSeconds.toDouble)
    }

  def strengths = ponderedHighlights(_.better)

  val weaknesses = ponderedHighlights(_.worse)

  // perfs with more games have more highlights
  def ponderedHighlights(compFilter: AnyComparison => Boolean)(nb: Int): List[(AnyComparison, PerfType)] =
    perfs
      .flatMap: p =>
        TutorCompare
          .sortAndPreventRepetitions(
            p.relevantComparisons.filter(compFilter)
          )(Math.ceil(nb.toDouble * p.stats.totalNbGames / nbGames).toInt)
          .map(_ -> p.perf)
      .sortBy(-_._1.grade.abs)
      .take(nb)

  override def toString = s"Report($user, $at, ${perfs.map(_.perf.key).mkString("+")})"

object TutorFullReport:

  val freshness = 1.day

  enum Availability:
    case Available(report: TutorFullReport, fresher: Option[TutorQueue.Status])
    case Empty(status: TutorQueue.Status)
    case InsufficientGames

  export Availability.*

  object F:
    val user = "user"
    val at = "at"
    val millis = "millis"
    val perfs = "perfs"
