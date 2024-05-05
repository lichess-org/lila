package lila.gathering

import chess.StartingPosition
import chess.format.Fen
import chess.opening.Eco

object Thematic:

  def byFen(fen: Fen.Standard): Option[StartingPosition] = fenIndex.get(fen)

  def byEco = ecoIndexForBc.get

  private lazy val fenIndex: Map[Fen.Standard, StartingPosition] = StartingPosition.all.mapBy(_.fen)
  private lazy val ecoIndexForBc: Map[Eco, StartingPosition]     = StartingPosition.all.mapBy(_.eco)
