package lila.coach

import org.joda.time.DateTime

import lila.rating.PerfType

case class UserStat(
    _id: String, // user ID
    openings: Openings,
    results: PerfResults,
    perfResults: PerfResults.PerfResultsMap,
    date: DateTime) {

  def id = _id

  def isFresh = false
  // results.base.nbGames < 100 || {
  //   DateTime.now minusDays 1 isBefore date
  // }
}

object UserStat {

  case class Computation(
      stat: UserStat,
      resultsComp: PerfResults.Computation,
      perfResultsComp: Map[PerfType, PerfResults.Computation],
      openingsComp: Openings.Computation) {

    def aggregate(p: RichPov) = copy(
      resultsComp = resultsComp.aggregate(p),
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
      perfResults = PerfResults.PerfResultsMap(perfResultsComp.mapValues(_.run)),
      openings = openingsComp.run)
  }
  def makeComputation(id: String) = Computation(apply(id), PerfResults.emptyComputation, Map.empty, Openings.emptyComputation)

  def apply(id: String): UserStat = UserStat(
    _id = id,
    openings = Openings.empty,
    results = PerfResults.empty,
    perfResults = PerfResults.emptyPerfResultsMap,
    date = DateTime.now)
}
