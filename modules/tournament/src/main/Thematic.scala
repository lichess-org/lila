package lila.tournament

import shogi.StartingPosition
import shogi.format.forsyth.Sfen

object Thematic {

  def bySfen(sfen: Sfen): Option[StartingPosition] = sfenIndex get sfen.value

  private lazy val sfenIndex: Map[String, StartingPosition] = StartingPosition.all.view.map { p =>
    p.sfen.value -> p
  }.toMap

}
