package lila.irwin

import org.joda.time.DateTime

import lila.report.{ SuspectId, ReporterId }
import lila.user.User

case class IrwinRequest(
    suspect: SuspectId,
    origin: IrwinRequest.Origin,
    date: DateTime
)

object IrwinRequest {

  sealed trait Origin {
    def key = toString.toLowerCase
  }

  object Origin {
    case object Moderator extends Origin
    case object Tournament extends Origin
    case object Leaderboard extends Origin
  }
}
