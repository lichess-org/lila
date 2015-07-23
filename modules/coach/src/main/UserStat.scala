package lila.coach

import org.joda.time.DateTime

import lila.rating.PerfType

case class UserStat(
    _id: String, // user ID
    colorResults: ColorResults,
    openings: Openings,
    results: PerfResults,
    perfResults: PerfResults.PerfResultsMap,
    date: DateTime) {

  def id = _id

  def isFresh = results.base.nbGames < 100 || {
    DateTime.now minusDays 1 isBefore date
  }
  def isStale = !isFresh

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
      openingsComp = openingsComp.aggregate(p)
    )

    def run = stat.copy(
      results = resultsComp.run,
      colorResults = colorResultsComp.run,
      perfResults = PerfResults.PerfResultsMap(perfResultsComp.mapValues(_.run)),
      openings = openingsComp.run)
  }
  def makeComputation(id: String) = Computation(empty(id), ColorResults.emptyComputation, PerfResults.emptyComputation, Map.empty, Openings.emptyComputation)

  def empty(id: String): UserStat = UserStat(
    _id = id,
    colorResults = ColorResults.empty,
    openings = Openings.empty,
    results = PerfResults.empty,
    perfResults = PerfResults.emptyPerfResultsMap,
    date = DateTime.now)
}
