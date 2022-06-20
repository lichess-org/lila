package lila.tutor

import chess.Color
import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.insight.InsightPerfStats
import lila.rating.PerfType
import lila.user.User

case class TutorReport(
    user: User.ID,
    at: DateTime,
    perfs: List[TutorPerfReport]
) {
  def apply(perfType: PerfType) = perfs.find(_.perf == perfType)
  def isFresh                   = at isAfter DateTime.now.minusMinutes(TutorReport.freshness.toMinutes.toInt)

  lazy val nbGames = perfs.toList.map(_.stats.nbGames).sum
  lazy val totalTime: FiniteDuration =
    perfs.toList.flatMap(_.estimateTotalTime).foldLeft(0.minutes)(_ + _)

  def favouritePerfs: List[TutorPerfReport] =
    perfs.head :: perfs.tail.takeWhile { perf =>
      perf.estimateTotalTime.exists(_ > totalTime * 0.25)
    }

  override def toString = s"Report($user, $at, ${perfs.map(_.perf.key) mkString "+"})"
}

case class TutorPerfReport(
    perf: PerfType,
    stats: InsightPerfStats,
    openings: Color.Map[TutorColorOpenings],
    phases: List[TutorPhase]
) {
  lazy val estimateTotalTime: Option[FiniteDuration] = (perf != PerfType.Correspondence) option stats.time * 2
}

object TutorReport {

  val freshness = 1 day

  sealed abstract class Availability
  case class Available(report: TutorReport, fresher: Option[TutorQueue.Status]) extends Availability {
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
