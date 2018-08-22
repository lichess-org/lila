package lila.irwin

import lila.report.Suspect
import lila.user.User
import lila.analyse.Analysis.Analyzed

case class IrwinRequest(
    suspect: Suspect,
    origin: IrwinRequest.Origin,
    games: List[Analyzed]
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
