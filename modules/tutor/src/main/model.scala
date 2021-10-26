package lila.tutor

import chess.{ Division, Situation }

import lila.analyse.Analysis
import lila.game.Pov
import lila.rating.PerfType

case class NbGames(value: Int) extends AnyVal with IntValue {
  def +(inc: Int)        = NbGames(value + inc)
  def inc(cond: Boolean) = if (cond) this + 1 else this
}
case class NbMoves(value: Int) extends AnyVal with IntValue {
  def +(inc: Int) = NbMoves(value + inc)
}
case class NbMovesRatio(a: Int, b: Int) {
  def +(ratio: NbMovesRatio) = NbMovesRatio(a + ratio.a, b + ratio.b)
}

case class PrePov(
    pov: Pov,
    perfType: PerfType,
    replay: List[Situation]
)

case class RichPov(
    pov: Pov,
    perfType: PerfType,
    replay: Vector[Situation],
    analysis: Option[Analysis],
    division: Division
) {
  def url = s"http://l.org/${pov.game.id}"
}
