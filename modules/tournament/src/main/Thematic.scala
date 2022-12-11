package lila.tournament

import chess.StartingPosition
import chess.format.Fen

object Thematic:

  def byFen(fen: Fen.Epd): Option[StartingPosition] = fenIndex get fen.value

  def byEco = ecoIndexForBc.get

  private lazy val fenIndex: Map[String, StartingPosition] = StartingPosition.all.view.map { p =>
    p.fen.value -> p
  }.toMap

  private lazy val ecoIndexForBc: Map[String, StartingPosition] = StartingPosition.all.view.map { p =>
    p.eco -> p
  }.toMap
