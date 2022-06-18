package lila.tutor

import lila.rating.PerfType
import lila.user.User
import org.joda.time.DateTime
import chess.Color

case class TutorReport(
    user: User.ID,
    at: DateTime,
    openings: Color.Map[TutorColorOpenings],
    phases: List[TutorPhase]
) {

  def isFresh = at isAfter DateTime.now.minusDays(1)
}

object TutorReport {}
