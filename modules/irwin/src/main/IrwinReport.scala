package lila.irwin

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
    gameId: String,
    activation: Int,
    blurs: Int,
    bot: Boolean,
    moves: List[MoveReport]
  )

  case class MoveReport(
    activation: Int,
    rank: Int, // selected PV, or null (if move is not in top 5)
    ambiguity: Int, // how many good moves are in the position
    odds: Int, // winning chances -100 -> 100
    loss: Int // percentage loss in winning chances
  )
}
