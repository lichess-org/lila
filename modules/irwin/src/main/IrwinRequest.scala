package lila.irwin

import lila.analyse.Analysis
import lila.report.Suspect

case class IrwinRequest(
    suspect: Suspect,
    origin: IrwinRequest.Origin,
    games: List[(Game, Option[Analysis])]
)

object IrwinRequest:

  enum Origin:
    case Moderator, Tournament, Leaderboard
    def key = Origin.this.toString.toLowerCase
