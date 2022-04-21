package lila.irwin

import org.joda.time.DateTime

import lila.game.{ Game, Pov }
import lila.report.SuspectId

case class IrwinReport(
    _id: String,     // user id
    activation: Int, // 0 = clean, 100 = cheater
    games: List[IrwinReport.GameReport],
    owner: String, // thread sending the report, for monitoring
    date: DateTime
) {

  def userId = _id

  def suspectId = SuspectId(userId)

  def note: String = games.sortBy(-_.activation).map { g =>
    s"#${g.gameId} = ${g.activation}"
  } mkString ", "

  def add(report: IrwinReport) = {
    val newIds = report.games.map(_.gameId).toSet
    report.copy(
      games = games.filterNot(g => newIds(g.gameId)) ::: report.games
    )
  }
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
      ambiguity: Int,    // how many good moves are in the position
      odds: Int,         // winning chances -100 -> 100
      loss: Int          // percentage loss in winning chances
  ) {
    override def toString =
      s"Rank: ${rank.fold("-")(_.toString)}, ambiguity: $ambiguity, odds: $odds, loss: $loss"
  }

  case class WithPovs(report: IrwinReport, povs: Map[Game.ID, Pov]) {

    def withPovs: List[GameReport.WithPov] =
      report.games.flatMap { gameReport =>
        povs get gameReport.gameId map { GameReport.WithPov(gameReport, _) }
      }
  }

  case class Dashboard(recent: List[IrwinReport]) {

    def lastSeenAt = recent.headOption.map(_.date)

    def seenRecently = lastSeenAt.??(DateTime.now.minusMinutes(15).isBefore)
  }
}
