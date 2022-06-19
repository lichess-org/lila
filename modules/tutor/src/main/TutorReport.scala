package lila.tutor

import chess.Color
import org.joda.time.DateTime

import lila.insight.PerfStats
import lila.rating.PerfType
import lila.user.User

case class TutorReport(
    user: User.ID,
    at: DateTime,
    perfs: List[TutorPerfReport]
) {
  def apply(perfType: PerfType) = perfs.find(_.perf == perfType)
  def isFresh                   = at isAfter DateTime.now.minusDays(1)
}

case class TutorPerfReport(
    perf: PerfType,
    stats: PerfStats,
    openings: Color.Map[TutorColorOpenings],
    phases: List[TutorPhase]
) {}
