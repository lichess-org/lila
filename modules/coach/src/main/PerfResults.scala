package lila.coach

import org.joda.time.DateTime

import chess.Status
import lila.game.Pov
import lila.rating.PerfType

case class PerfResults(
    base: Results,
    bestRating: Option[PerfResults.BestRating],
    winStreak: PerfResults.Streak, // nb games won in a row
    awakeMinutesStreak: PerfResults.Streak, // minutes played without sleeping
    dayStreak: PerfResults.Streak, // days played in a row
    outcomeStatuses: PerfResults.OutcomeStatuses) {

  def aggregate(p: RichPov) = copy(
    bestRating = if (~p.pov.win) {
      PerfResults.makeBestRating(p.pov).fold(bestRating) { newBest =>
        bestRating.fold(newBest) { prev =>
          if (newBest.rating > prev.rating) newBest else prev
        }.some
      }
    }
    else bestRating,
    outcomeStatuses = outcomeStatuses.aggregate(p))
}

object PerfResults {

  case class BestRating(id: String, userId: String, rating: Int)
  def makeBestRating(pov: Pov): Option[BestRating] =
    pov.opponent.userId |@| pov.player.ratingAfter apply {
      case (opId, myRating) => BestRating(pov.gameId, opId, myRating)
    }

  case class PerfResultsMap(m: Map[PerfType, PerfResults]) {
    def sorted: List[(PerfType, PerfResults)] = m.toList.sortBy(-_._2.base.nbGames)
  }
  val emptyPerfResultsMap = PerfResultsMap(Map.empty)

  case class StatusScores(m: Map[Status, Int]) {
    def add(s: Status) = copy(m = m + (s -> m.get(s).fold(1)(1+)))
    def sorted: List[(Status, Int)] = m.toList.sortBy(-_._2)
    lazy val sum: Int = m.foldLeft(0)(_ + _._2)
  }

  case class OutcomeStatuses(win: StatusScores, loss: StatusScores) {
    def aggregate(p: RichPov) = copy(
      win = if (~p.pov.win) win add p.pov.game.status else win,
      loss = if (~p.pov.loss) loss add p.pov.game.status else loss)
  }
  val emptyOutcomeStatuses = OutcomeStatuses(StatusScores(Map.empty), StatusScores(Map.empty))

  case class Streak(cur: Int, best: Int) {
    def add(v: Int) = copy(cur = cur + v, best = best max (cur + v))
    def reset = copy(cur = 0)
    def set(v: Int) = copy(cur = v)
  }
  val emptyStreak = Streak(0, 0)

  val empty = PerfResults(Results.empty, none, emptyStreak, emptyStreak, emptyStreak, emptyOutcomeStatuses)

  case class Computation(
      results: PerfResults,
      base: Results.Computation,
      previousEndDate: Option[DateTime],
      previousWin: Boolean) {

    def aggregate(p: RichPov) = copy(
      results = results.aggregate(p).copy(
        winStreak = if (~p.pov.win) {
          if (previousWin) results.winStreak.add(1)
          else results.winStreak.set(1)
        }
        else results.winStreak.reset,
        awakeMinutesStreak = results.awakeMinutesStreak,
        dayStreak = (previousEndDate |@| p.pov.game.updatedAt) apply {
          case (prev, next) if prev.getDayOfYear == next.getDayOfYear => results.dayStreak
          case (prev, next) if next.minusDays(1).isBefore(prev) => results.dayStreak.add(1)
          case _ => results.dayStreak.reset
        } getOrElse results.dayStreak.reset
      ),
      base = base.aggregate(p),
      previousEndDate = p.pov.game.updatedAt,
      previousWin = ~p.pov.win)

    def run = results.copy(base = base.run)
  }
  val emptyComputation = Computation(empty, Results.emptyComputation, none, false)
}
