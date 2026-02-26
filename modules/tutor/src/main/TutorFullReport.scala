package lila.tutor

import lila.rating.PerfType
import lila.tutor.TutorCompare.AnyComparison

case class TutorFullReport(
    config: TutorConfig,
    at: Instant,
    perfs: List[TutorPerfReport]
):
  def apply(perfType: PerfType) = perfs.find(_.perf == perfType)

  export config.{ user, id, url }

  def stats = perfs.map(_.stats)

  lazy val nbGames = stats.totalNbGames

  lazy val totalTime: FiniteDuration =
    perfs.flatMap(_.estimateTotalTime).foldLeft(0.minutes)(_ + _)

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

  override def toString = s"Report($config, $at, ${perfs.map(_.perf.key).mkString("+")})"

object TutorFullReport:

  case class Preview(config: TutorConfig, at: Instant, perfs: List[TutorPerfReport.Preview]):
    def stats = perfs.map(_.stats)

  object F:
    val config = "config"
    val user = s"$config.user"
    val at = "at"
    val millis = "millis"
    val perfs = "perfs"
