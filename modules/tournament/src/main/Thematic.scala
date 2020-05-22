package lila.tournament

import chess.StartingPosition

private object Thematic {

  def byFen = fenIndex.get _
  def byEco = ecoIndexForBc.get _

  private lazy val fenIndex: Map[String, StartingPosition] = StartingPosition.all.view.map { p =>
    p.fen -> p
  }.toMap

  private lazy val ecoIndexForBc: Map[String, StartingPosition] = StartingPosition.all.view.map { p =>
    p.eco -> p
  }.toMap
}
