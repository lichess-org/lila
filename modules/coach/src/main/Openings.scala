package lila.coach

import chess.Color
import lila.analyse.Analysis
import lila.game.Pov

case class Openings(
    white: Openings.OpeningsMap,
    black: Openings.OpeningsMap) {

  def apply(c: Color) = c.fold(white, black)
}

object Openings {

  case class OpeningsMap(m: Map[String, Results]) {
    def best(max: Int): List[(String, Results)] = m.toList.sortBy(-_._2.nbGames) take max
    def trim(max: Int) = copy(m = best(max).toMap)
  }
  val emptyOpeningsMap = OpeningsMap(Map.empty)

  val empty = Openings(emptyOpeningsMap, emptyOpeningsMap)

  case class Computation(
      white: Map[String, Results.Computation],
      black: Map[String, Results.Computation]) {

    def aggregate(p: RichPov) =
      p.pov.game.opening.map(_.code).fold(this) { code =>
        copy(
          white = if (p.pov.color.white) agg(white, code, p) else white,
          black = if (p.pov.color.black) agg(black, code, p) else black)
      }

    private def agg(ops: Map[String, Results.Computation], code: String, p: RichPov) =
      ops + (code -> ops.get(code).|(Results.emptyComputation).aggregate(p))

    def run = Openings(
      white = OpeningsMap(white.mapValues(_.run)) trim 10,
      black = OpeningsMap(black.mapValues(_.run)) trim 10)
  }
  val emptyComputation = Computation(Map.empty, Map.empty)
}
