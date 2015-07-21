package lila.coach

import org.joda.time.DateTime

import lila.game.Pov

case class Results(
    nbGames: Int,
    nbAnalysis: Int,
    nbWin: Int,
    nbLoss: Int,
    nbDraw: Int,
    ratingDiff: Int,
    gameSections: GameSections,
    moves: Moves,
    bestWin: Option[Results.BestWin],
    opponentRatingSum: Int,
    lastPlayed: Option[DateTime]) {

  def opponentRatingAvg = (nbGames > 0) option (opponentRatingSum / nbGames)

  def ratingDiffAvg = (nbGames > 0) option (ratingDiff / nbGames)

  def aggregate(p: RichPov) = copy(
    nbGames = nbGames + 1,
    nbAnalysis = nbAnalysis + p.analysis.isDefined.fold(1, 0),
    nbWin = nbWin + (~p.pov.win).fold(1, 0),
    nbLoss = nbLoss + (~p.pov.loss).fold(1, 0),
    nbDraw = nbDraw + p.pov.game.drawn.fold(1, 0),
    ratingDiff = ratingDiff + ~p.pov.player.ratingDiff,
    gameSections = gameSections aggregate p,
    moves = moves aggregate p,
    bestWin = if (~p.pov.win) {
      Results.makeBestWin(p.pov).fold(bestWin) { newBest =>
        bestWin.fold(newBest) { prev =>
          if (newBest.rating > prev.rating) newBest else prev
        }.some
      }
    }
    else bestWin,
    opponentRatingSum = opponentRatingSum + ~p.pov.opponent.rating,
    lastPlayed = lastPlayed orElse p.pov.game.createdAt.some)

  def merge(r: Results) = Results(
    nbGames = nbGames + r.nbGames,
    nbAnalysis = nbAnalysis + r.nbAnalysis,
    nbWin = nbWin + r.nbWin,
    nbLoss = nbLoss + r.nbLoss,
    nbDraw = nbDraw + r.nbDraw,
    ratingDiff = ratingDiff + r.ratingDiff,
    gameSections = gameSections merge r.gameSections,
    moves = moves merge r.moves,
    bestWin = (bestWin, r.bestWin) match {
      case (Some(a), Some(b)) => Some(a merge b)
      case (Some(a), _)       => a.some
      case (_, Some(b))       => b.some
      case _                  => none
    },
    opponentRatingSum = opponentRatingSum + r.opponentRatingSum,
    lastPlayed = (lastPlayed, r.lastPlayed) match {
      case (Some(a), Some(b)) => Some(if (a isAfter b) a else b)
      case (Some(a), _)       => a.some
      case (_, Some(b))       => b.some
      case _                  => none
    })
}

object Results {

  val empty = Results(0, 0, 0, 0, 0, 0, GameSections.empty, Moves.empty, none, 0, none)

  case class BestWin(id: String, userId: String, rating: Int) {
    def merge(b: BestWin) = if (rating > b.rating) this else b
  }
  def makeBestWin(pov: Pov): Option[BestWin] = (pov.game.playedTurns > 4) ?? {
    pov.opponent.userId |@| pov.opponent.rating apply {
      case (opId, opRating) => BestWin(pov.gameId, opId, opRating)
    }
  }

  case class Computation(results: Results) {

    def aggregate(p: RichPov) = copy(results = results.aggregate(p))

    def run = results
  }
  val emptyComputation = Computation(empty)
}
