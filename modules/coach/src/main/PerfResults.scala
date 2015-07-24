package lila.coach

import org.joda.time.DateTime

import chess.Status
import lila.game.Pov
import lila.rating.PerfType

case class PerfResults(
    base: Results,
    bestRating: Option[PerfResults.BestRating],
    // winStreak: PerfResults.Streak, // nb games won in a row
    // awakeMinutesStreak: PerfResults.Streak, // minutes played without sleeping
    // dayStreak: PerfResults.Streak, // days played in a row
    outcomeStatuses: PerfResults.OutcomeStatuses) {

  def aggregate(p: RichPov) = copy(
    base = base aggregate p,
    bestRating = if (~p.pov.win) {
      PerfResults.makeBestRating(p.pov).fold(bestRating) { newBest =>
        bestRating.fold(newBest) { prev =>
          if (newBest.rating > prev.rating) newBest else prev
        }.some
      }
    }
    else bestRating,
    outcomeStatuses = outcomeStatuses.aggregate(p))

  def merge(o: PerfResults) = PerfResults(
    base = base merge o.base,
    bestRating = (bestRating, o.bestRating) match {
      case (Some(a), Some(b)) => Some(a merge b)
      case (a, b)             => a orElse b
    },
    outcomeStatuses = outcomeStatuses merge o.outcomeStatuses)
}

object PerfResults {

  case class BestRating(id: String, userId: String, rating: Int) {
    def merge(o: BestRating) = if (rating >= o.rating) this else o
  }
  def makeBestRating(pov: Pov): Option[BestRating] =
    pov.opponent.userId |@| pov.player.ratingAfter apply {
      case (opId, myRating) => BestRating(pov.gameId, opId, myRating)
    }

  case class PerfResultsMap(m: Map[PerfType, PerfResults]) {
    def sorted: List[(PerfType, PerfResults)] = m.toList.sortBy(-_._2.base.nbGames)
    def merge(o: PerfResultsMap) = PerfResultsMap {
      m.map {
        case (k, v) => k -> o.m.get(k).fold(v)(v.merge)
      }
    }
  }
  val emptyPerfResultsMap = PerfResultsMap(Map.empty)

  case class StatusScores(m: Map[Status, Int]) {
    def aggregate(s: Status) = copy(m = m + (s -> m.get(s).fold(1)(1+)))
    def merge(o: StatusScores) = StatusScores {
      m.map {
        case (k, v) => k -> (v + ~o.m.get(k))
      }
    }
    def sorted: List[(Status, Int)] = m.toList.sortBy(-_._2)
    lazy val sum: Int = m.foldLeft(0)(_ + _._2)
  }

  case class OutcomeStatuses(win: StatusScores, loss: StatusScores) {
    def aggregate(p: RichPov) = copy(
      win = if (~p.pov.win) win aggregate p.pov.game.status else win,
      loss = if (~p.pov.loss) loss aggregate p.pov.game.status else loss)
    def merge(o: OutcomeStatuses) = OutcomeStatuses(
      win = win merge o.win,
      loss = loss merge o.win)
  }
  val emptyOutcomeStatuses = OutcomeStatuses(StatusScores(Map.empty), StatusScores(Map.empty))

  val empty = PerfResults(Results.empty, none, emptyOutcomeStatuses)

  case class Computation(
      results: PerfResults,
      base: Results.Computation) {

    def aggregate(p: RichPov) = copy(
      results = results.aggregate(p),
      base = base.aggregate(p))

    def nbGames = base.nbGames

    def run = results.copy(base = base.run)
  }
  val emptyComputation = Computation(empty, Results.emptyComputation)
}
