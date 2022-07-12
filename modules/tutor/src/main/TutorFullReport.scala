package lila.tutor

import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.common.Heapsort.implicits._
import lila.rating.PerfType
import lila.user.User
import lila.tutor.TutorCompare.AnyComparison

case class TutorFullReport(
    user: User.ID,
    at: DateTime,
    perfs: List[TutorPerfReport]
) {
  def apply(perfType: PerfType) = perfs.find(_.perf == perfType)
  def isFresh = at isAfter DateTime.now.minusMinutes(TutorFullReport.freshness.toMinutes.toInt)

  lazy val nbGames = perfs.toList.map(_.stats.totalNbGames).sum
  lazy val totalTime: FiniteDuration =
    perfs.toList.flatMap(_.estimateTotalTime).foldLeft(0.minutes)(_ + _)

  def favouritePerfs: List[TutorPerfReport] = perfs.headOption ?? {
    _ :: perfs.tailSafe.takeWhile { perf =>
      perf.estimateTotalTime.exists(_ > totalTime * 0.25)
    }
  }

  def percentTimeOf(perf: PerfType): Option[GoodPercent] =
    apply(perf).flatMap(_.estimateTotalTime) map { time =>
      GoodPercent(time.toSeconds.toDouble, totalTime.toSeconds.toDouble)
    }

  def strengths = ponderedHighlights(_.better) _

  val weaknesses = ponderedHighlights(_.worse) _

  // perfs with more games have more highlights
  def ponderedHighlights(compFilter: AnyComparison => Boolean)(nb: Int): List[(AnyComparison, PerfType)] =
    perfs
      .flatMap { p =>
        TutorCompare.sortAndPreventRepetitions(
          p.relevantComparisons.filter(compFilter)
        )(Math.ceil(nb.toDouble * p.stats.totalNbGames / nbGames).toInt) map (_ -> p.perf)
      }
      .sortBy(-_._1.grade.abs)
      .take(nb)

  override def toString = s"Report($user, $at, ${perfs.map(_.perf.key) mkString "+"})"
}

object TutorFullReport {

  val freshness = 1 day

  sealed abstract class Availability
  case class Available(report: TutorFullReport, fresher: Option[TutorQueue.Status]) extends Availability {
    def isFresh = fresher.isEmpty
  }
  case class Empty(status: TutorQueue.Status) extends Availability
  case object InsufficientGames               extends Availability

  object F {
    val user   = "user"
    val at     = "at"
    val millis = "millis"
  }
}
