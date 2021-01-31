package lila.tournament

import chess.StartingPosition
import chess.format.FEN

object Thematic {

  def byFen(fen: FEN): Option[StartingPosition] = fenIndex get fen.value

  def byEco = ecoIndexForBc.get _

  private lazy val fenIndex: Map[String, StartingPosition] = StartingPosition.all.view.map { p =>
    p.fen.value -> p
  }.toMap

  private lazy val ecoIndexForBc: Map[String, StartingPosition] = StartingPosition.all.view.map { p =>
    p.eco -> p
  }.toMap
}
