package lila.tournament

import chess.StartingPosition
import chess.format.Fen

object Thematic:

  def byFen(fen: Fen.Epd): Option[StartingPosition] = fenIndex get fen

  def byEco = ecoIndexForBc.get

  private lazy val fenIndex: Map[Fen.Epd, StartingPosition]     = StartingPosition.all.mapBy(_.fen)
  private lazy val ecoIndexForBc: Map[String, StartingPosition] = StartingPosition.all.mapBy(_.eco)
