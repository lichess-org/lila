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
    def nbGames = m.foldLeft(0) {
      case (nb, (_, results)) => nb + results.nbGames
    }
  }
  val emptyOpeningsMap = OpeningsMap(Map.empty)

  val empty = Openings(emptyOpeningsMap, emptyOpeningsMap)

  case class Computation(
      white: Map[String, Results.Computation],
      black: Map[String, Results.Computation]) {

    def aggregate(p: RichPov) =
      Ecopening(p.pov.game).map(_.name).fold(this) { name =>
        copy(
          white = if (p.pov.color.white) agg(white, name, p) else white,
          black = if (p.pov.color.black) agg(black, name, p) else black)
      }

    private def agg(ops: Map[String, Results.Computation], name: String, p: RichPov) =
      ops + (name -> ops.get(name).|(Results.emptyComputation).aggregate(p))

    def run = Openings(
      white = OpeningsMap(white.mapValues(_.run)) trim 15,
      black = OpeningsMap(black.mapValues(_.run)) trim 15)
  }
  val emptyComputation = Computation(Map.empty, Map.empty)
}
