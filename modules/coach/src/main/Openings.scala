package lila.coach

import chess.Color
import lila.analyse.Analysis
import lila.game.Pov

case class Openings(
    white: Openings.OpeningsMap,
    black: Openings.OpeningsMap) {

  def apply(c: Color) = c.fold(white, black)

  def all = white ++ black
}

object Openings {

  case class OpeningsMap(m: Map[String, Results]) {
    def best(max: Int): List[(String, Results)] = m.toList.sortBy(-_._2.nbGames) take max
    def trim(max: Int) = copy(m = best(max).toMap)
    def ++(other: OpeningsMap) = OpeningsMap(m ++ other.m)
  }
  val emptyOpeningsMap = OpeningsMap(Map.empty)

  val empty = Openings(emptyOpeningsMap, emptyOpeningsMap)

  case class Computation(
      white: Map[String, Results.Computation],
      black: Map[String, Results.Computation]) {

    def aggregate(p: RichPov) =
      p.pov.game.opening.map(_.familyName).fold(this) { familyName =>
        copy(
          white = if (p.pov.color.white) agg(white, familyName, p) else white,
          black = if (p.pov.color.black) agg(black, familyName, p) else black)
      }

    private def agg(ops: Map[String, Results.Computation], familyName: String, p: RichPov) =
      ops + (familyName -> ops.get(familyName).|(Results.emptyComputation).aggregate(p))

    def run = Openings(
      white = OpeningsMap(white.mapValues(_.run)) trim 15,
      black = OpeningsMap(black.mapValues(_.run)) trim 15)
  }
  val emptyComputation = Computation(Map.empty, Map.empty)
}
