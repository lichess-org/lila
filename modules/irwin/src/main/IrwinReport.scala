package lila.irwin

import lila.game.{ Game, Pov }

import org.joda.time.DateTime

case class IrwinReport(
    _id: String, // user id
    isLegit: Option[Boolean],
    activation: Int, // 0 = clean, 100 = cheater
    games: List[IrwinReport.GameReport],
    date: DateTime
) {

  def id = _id
}

object IrwinReport {

  case class GameReport(
    gameId: Game.ID,
    activation: Int,
    moves: List[MoveReport]
  )

  object GameReport {

    case class WithPov(report: GameReport, pov: Pov)
  }

  case class MoveReport(
    activation: Int,
    rank: Option[Int], // selected PV, or null (if move is not in top 5)
    ambiguity: Int, // how many good moves are in the position
    odds: Int, // winning chances -100 -> 100
    loss: Int // percentage loss in winning chances
  )

  case class WithPovs(report: IrwinReport, povs: Map[Game.ID, Pov]) {

    def withPovs: List[GameReport.WithPov] = report.games.flatMap { gameReport =>
      povs get gameReport.gameId map { GameReport.WithPov(gameReport, _) }
    }
  }
}
