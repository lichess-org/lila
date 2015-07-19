package lila.coach

import org.joda.time.DateTime

import chess.Status
import lila.rating.PerfType

case class PerfResults(
    base: Results,
    awakeMinutesStreak: Results.Streak, // minutes played without sleeping
    dayStreak: Results.Streak, // days played in a row
    outcomeStatuses: PerfResults.OutcomeStatuses) {

  def aggregate(p: RichPov) = copy(
    outcomeStatuses = outcomeStatuses.aggregate(p))
}

object PerfResults {

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

  val empty = PerfResults(Results.empty, Results.emptyStreak, Results.emptyStreak, emptyOutcomeStatuses)

  case class Computation(
      results: PerfResults,
      base: Results.Computation,
      previousEndDate: Option[DateTime]) {

    def aggregate(p: RichPov) = copy(
      results = results.aggregate(p).copy(
        awakeMinutesStreak = results.awakeMinutesStreak,
        dayStreak = (previousEndDate |@| p.pov.game.updatedAt) apply {
          case (prev, next) if prev.getDayOfYear == next.getDayOfYear => results.dayStreak
          case (prev, next) if next.minusDays(1).isBefore(prev) => results.dayStreak.add(1)
          case _ => results.dayStreak.reset
        } getOrElse results.dayStreak.reset
      ),
      base = base.aggregate(p),
      previousEndDate = p.pov.game.updatedAt)

    def run = results.copy(base = base.run)
  }
  val emptyComputation = Computation(empty, Results.emptyComputation, none)
}
