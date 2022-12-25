package lila.irwin

import lila.report.Suspect
import lila.game.Game
import lila.analyse.Analysis

case class IrwinRequest(
    suspect: Suspect,
    origin: IrwinRequest.Origin,
    games: List[(Game, Option[Analysis])]
)

object IrwinRequest:

  enum Origin:
    case Moderator, Tournament, Leaderboard
    def key = Origin.this.toString.toLowerCase
