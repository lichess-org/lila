package lila.coach

import org.joda.time.DateTime

import lila.rating.PerfType

case class UserStat(
    colorResults: ColorResults,
    openings: Openings,
    results: PerfResults,
    perfResults: PerfResults.PerfResultsMap) {

  def merge(o: UserStat) = copy(
    colorResults = colorResults merge o.colorResults,
    openings = openings merge o.openings,
    results = results merge o.results,
    perfResults = perfResults merge o.perfResults)
}

object UserStat {

  case class Computation(
      stat: UserStat,
      colorResultsComp: ColorResults.Computation,
      resultsComp: PerfResults.Computation,
      perfResultsComp: Map[PerfType, PerfResults.Computation],
      openingsComp: Openings.Computation) {

    def aggregate(p: RichPov) = copy(
      resultsComp = resultsComp aggregate p,
      colorResultsComp = colorResultsComp aggregate p,
      perfResultsComp = p.pov.game.perfType.fold(perfResultsComp) { perfType =>
        perfResultsComp + (
          perfType ->
          (perfResultsComp.get(perfType) | PerfResults.emptyComputation).aggregate(p)
        )
      },
      openingsComp = openingsComp.aggregate(p))

    def nbGames = resultsComp.nbGames

    def run = stat.copy(
      results = resultsComp.run,
      colorResults = colorResultsComp.run,
      perfResults = PerfResults.PerfResultsMap(perfResultsComp.mapValues(_.run)),
      openings = openingsComp.run)
  }

  val empty = UserStat(
    colorResults = ColorResults.empty,
    openings = Openings.empty,
    results = PerfResults.empty,
    perfResults = PerfResults.emptyPerfResultsMap)

  val emptyComputation = Computation(empty, ColorResults.emptyComputation, PerfResults.emptyComputation, Map.empty, Openings.emptyComputation)
}
