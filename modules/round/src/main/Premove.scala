package lila.round

import org.joda.time.DateTime

import lila.game.Game

case class Premove(
  _id: String, // player full id
  ply: Int,
  uciMoves: List[String],
  date: DateTime)

object Premove {

  def gameQualifies(g: Game) = g.isCorrespondence && !g.hasAi
}
