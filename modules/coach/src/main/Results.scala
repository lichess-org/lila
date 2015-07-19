package lila.coach

import lila.game.Pov

case class Results(
    nbGames: Int,
    nbAnalysis: Int,
    nbWin: Int,
    nbLoss: Int,
    nbDraw: Int,
    ratingDiff: Int,
    gameSections: GameSections,
    bestWin: Option[Results.BestWin],
    bestRating: Option[Results.BestRating],
    opponentRatingSum: Int,
    winStreak: Results.Streak // nb games won in a row
    ) {

  private def safeDiv(x: Int, y: Int) = if (y == 0) 0 else x / y

  def opponentRatingAvg = safeDiv(opponentRatingSum, nbGames)

  def aggregate(p: RichPov) = copy(
    nbGames = nbGames + 1,
    nbAnalysis = nbAnalysis + p.analysis.isDefined.fold(1, 0),
    nbWin = nbWin + (~p.pov.win).fold(1, 0),
    nbLoss = nbLoss + (~p.pov.loss).fold(1, 0),
    nbDraw = nbDraw + p.pov.game.draw.fold(1, 0),
    ratingDiff = ratingDiff + ~p.pov.player.ratingDiff,
    gameSections = gameSections aggregate p,
    bestWin = if (~p.pov.win) {
      Results.makeBestWin(p.pov).fold(bestWin) { newBest =>
        bestWin.fold(newBest) { prev =>
          if (newBest.rating > prev.rating) newBest else prev
        }.some
      }
    }
    else bestWin,
    bestRating = if (~p.pov.win) {
      Results.makeBestRating(p.pov).fold(bestRating) { newBest =>
        bestRating.fold(newBest) { prev =>
          if (newBest.rating > prev.rating) newBest else prev
        }.some
      }
    }
    else bestRating,
    opponentRatingSum = opponentRatingSum + ~p.pov.opponent.rating)
}

object Results {

  val emptyStreak = Streak(0, 0)
  val empty = Results(0, 0, 0, 0, 0, 0, GameSections.empty, none, none, 0, emptyStreak)

  case class BestWin(id: String, userId: String, rating: Int)
  def makeBestWin(pov: Pov): Option[BestWin] = (pov.game.playedTurns > 4) ?? {
    pov.opponent.userId |@| pov.opponent.rating apply {
      case (opId, opRating) => BestWin(pov.gameId, opId, opRating)
    }
  }

  case class BestRating(id: String, userId: String, rating: Int)
  def makeBestRating(pov: Pov): Option[BestRating] = pov.opponent.userId |@| pov.player.ratingAfter apply {
    case (opId, myRating) => BestRating(pov.gameId, opId, myRating)
  }

  case class Streak(cur: Int, best: Int) {
    def add(v: Int) = copy(cur = cur + v, best = best max (cur + v))
    def reset = copy(cur = 0)
    def set(v: Int) = copy(cur = v)
  }

  case class Computation(results: Results, previousWin: Boolean) {

    def aggregate(p: RichPov) = copy(
      results = results.aggregate(p).copy(
        winStreak = if (~p.pov.win) {
          if (previousWin) results.winStreak.add(1)
          else results.winStreak.set(1)
        }
        else results.winStreak.reset
      ),
      previousWin = ~p.pov.win)

    def run = results
  }
  val emptyComputation = Computation(empty, false)
}
