package lila.coach

import chess.Status
import lila.analyse.Analysis
import lila.game.Pov
import org.joda.time.DateTime

case class Results(
    nbGames: Int,
    nbAnalysis: Int,
    nbWin: Int,
    nbLoss: Int,
    nbDraw: Int,
    ratingDiff: Int,
    plySum: Int,
    acplSum: Int,
    bestWin: Option[Results.BestWin],
    bestRating: Option[Results.BestRating],
    opponentRatingSum: Int,
    winStreak: Results.Streak, // nb games won in a row
    awakeMinutesStreak: Results.Streak, // minutes played without sleeping
    dayStreak: Results.Streak, // days played in a row
    outcomeStatuses: Results.OutcomeStatuses) {

  def plyAvg = plySum / nbGames
  def acplAvg = acplSum / nbAnalysis
  def opponentRatingAvg = opponentRatingSum / nbGames

  def aggregate(pov: Pov, analysis: Option[Analysis]) = copy(
    nbGames = nbGames + 1,
    nbAnalysis = nbAnalysis + analysis.isDefined.fold(1, 0),
    nbWin = nbWin + (~pov.win).fold(1, 0),
    nbLoss = nbLoss + (~pov.loss).fold(1, 0),
    nbDraw = nbDraw + pov.game.draw.fold(1, 0),
    ratingDiff = ratingDiff + ~pov.player.ratingDiff,
    plySum = plySum + pov.game.turns,
    acplSum = acplSum + ~analysis.flatMap { lila.analyse.Accuracy(pov, _) },
    bestWin = if (~pov.win) {
      Results.makeBestWin(pov).fold(bestWin) { newBest =>
        bestWin.fold(newBest) { prev =>
          if (newBest.rating > prev.rating) newBest else prev
        }.some
      }
    }
    else bestWin,
    bestRating = if (~pov.win) {
      Results.makeBestRating(pov).fold(bestRating) { newBest =>
        bestRating.fold(newBest) { prev =>
          if (newBest.rating > prev.rating) newBest else prev
        }.some
      }
    }
    else bestRating,
    opponentRatingSum = opponentRatingSum + ~pov.opponent.rating,
    outcomeStatuses = outcomeStatuses.aggregate(pov))
}

object Results {

  val emptyStreak = Streak(0, 0)
  val emptyOutcomeStatuses = OutcomeStatuses(StatusScores(Map.empty), StatusScores(Map.empty))
  val empty = Results(0, 0, 0, 0, 0, 0, 0, 0, none, none, 0, emptyStreak, emptyStreak, emptyStreak, emptyOutcomeStatuses)

  case class BestWin(id: String, userId: String, rating: Int)
  def makeBestWin(pov: Pov): Option[BestWin] = pov.opponent.userId |@| pov.opponent.rating apply {
    case (opId, opRating) => BestWin(pov.gameId, opId, opRating)
  }

  case class BestRating(id: String, userId: String, rating: Int)
  def makeBestRating(pov: Pov): Option[BestRating] = pov.opponent.userId |@| pov.player.ratingAfter apply {
    case (opId, myRating) => BestRating(pov.gameId, opId, myRating)
  }

  case class OutcomeStatuses(win: StatusScores, loss: StatusScores) {
    def aggregate(pov: Pov) = copy(
      win = if (~pov.win) win add pov.game.status else win,
      loss = if (~pov.loss) loss add pov.game.status else loss)
  }

  case class StatusScores(m: Map[Status, Int]) {
    def add(s: Status) = copy(m = m + (s -> m.get(s).fold(1)(1+)))
  }

  case class Streak(cur: Int, best: Int) {
    def add(v: Int) = copy(cur = cur + v, best = best max (cur + v))
    def reset = copy(cur = 0)
    def set(v: Int) = copy(cur = v)
  }

  case class Computation(
      results: Results,
      previousWin: Boolean = false,
      previousEndDate: Option[DateTime] = None) {

    def aggregate(pov: Pov, analysis: Option[Analysis]) = copy(
      results = results.aggregate(pov, analysis).copy(
        winStreak = if (~pov.win) {
          if (previousWin) results.winStreak.add(1)
          else results.winStreak.set(1)
        }
        else results.winStreak.reset,
        awakeMinutesStreak = results.awakeMinutesStreak,
        dayStreak = (previousEndDate |@| pov.game.updatedAt) apply {
          case (prev, next) if prev.getDayOfYear == next.getDayOfYear => results.dayStreak
          case (prev, next) if next.minusDays(1).isBefore(prev)       => results.dayStreak.add(1)
        } getOrElse results.dayStreak.reset
      ),
      previousWin = ~pov.win,
      previousEndDate = pov.game.updatedAt)

    def run = results
  }
  val emptyComputation = Computation(empty)
}
