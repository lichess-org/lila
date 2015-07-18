package lila.coach

import org.joda.time.DateTime

import lila.analyse.Analysis
import lila.game.Pov
import lila.rating.PerfType

case class UserStat(
    _id: String, // user ID
    openings: Openings,
    results: Results,
    perfResults: UserStat.PerfResults,
    date: DateTime) {

  def id = _id

  def aggregate(pov: Pov, analysis: Option[lila.analyse.Analysis]) = copy(
    openings = openings aggregate pov)

  def isFresh = results.nbGames < 100 || {
    DateTime.now minusDays 1 isBefore date
  }
}

object UserStat {

  case class PerfResults(m: Map[PerfType, Results])
  val emptyPerfResults = PerfResults(Map.empty)

  case class Computation(
      stat: UserStat,
      resultsComp: Results.Computation = Results.emptyComputation,
      perfResultsComp: Map[PerfType, Results.Computation] = Map.empty) {

    def aggregate(pov: Pov, analysis: Option[Analysis]) = copy(
      stat = stat.aggregate(pov, analysis),
      resultsComp = resultsComp.aggregate(pov, analysis),
      perfResultsComp = pov.game.perfType.fold(perfResultsComp) { perfType =>
        perfResultsComp + (
          perfType ->
          perfResultsComp.get(perfType).|(Results.emptyComputation).aggregate(pov, analysis)
        )
      }
    )

    def run = stat.copy(
      results = resultsComp.run,
      perfResults = PerfResults(perfResultsComp.mapValues(_.run)))
  }
  def makeComputation(id: String) = Computation(apply(id))

  def apply(id: String): UserStat = UserStat(
    _id = id,
    openings = Openings(Map.empty, Map.empty),
    results = Results.empty,
    perfResults = emptyPerfResults,
    date = DateTime.now)
}
