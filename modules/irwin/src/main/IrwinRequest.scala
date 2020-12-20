package lila.irwin

import lila.report.Suspect
import lila.game.Game
import lila.analyse.Analysis

case class IrwinRequest(
    suspect: Suspect,
    origin: IrwinRequest.Origin,
    games: List[(Game, Option[Analysis])]
)

object IrwinRequest {

  sealed trait Origin {
    def key = toString.toLowerCase
  }

  object Origin {
    case object Moderator   extends Origin
    case object Tournament  extends Origin
    case object Leaderboard extends Origin
  }
}
