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
  // def isFresh                   = at isAfter DateTime.now.minusDays(1)

  lazy val nbGames                   = perfs.map(_.stats.nbGames).sum
  lazy val totalTime: FiniteDuration = perfs.foldLeft(0.minutes)(_ + _.estimateTotalTime)

  def favouritePerfs: List[TutorPerfReport] =
    perfs.takeWhile(_.estimateTotalTime > totalTime * 0.25)
}

case class TutorPerfReport(
    perf: PerfType,
    stats: InsightPerfStats,
    openings: Color.Map[TutorColorOpenings],
    phases: List[TutorPhase]
) {
  lazy val estimateTotalTime = (perf != PerfType.Correspondence) option stats.time * 2
}
