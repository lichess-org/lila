package lila.tournament

import chess.StartingPosition
import chess.opening.Eco
import chess.format.Fen

object Thematic:

  def byFen(fen: Fen.Opening): Option[StartingPosition] = fenIndex get fen

  def byEco = ecoIndexForBc.get

  private lazy val fenIndex: Map[Fen.Opening, StartingPosition] = StartingPosition.all.mapBy(_.fen)
  private lazy val ecoIndexForBc: Map[Eco, StartingPosition]    = StartingPosition.all.mapBy(_.eco)
