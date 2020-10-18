package lila.tournament

import chess.StartingPosition
import chess.format.FEN

object Thematic {

  def byFen                                     = fenIndex.get _
  def byFen(fen: FEN): Option[StartingPosition] = byFen(fen.value)
  def byEco                                     = ecoIndexForBc.get _

  private lazy val fenIndex: Map[String, StartingPosition] = StartingPosition.all.view.map { p =>
    p.fen -> p
  }.toMap

  private lazy val ecoIndexForBc: Map[String, StartingPosition] = StartingPosition.all.view.map { p =>
    p.eco -> p
  }.toMap
}
