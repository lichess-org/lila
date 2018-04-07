package lila.puzzle

import scala.util.Random

import lila.db.dsl._
import lila.user.User
import lila.rating.Perf
import Puzzle.{ BSONFields => F }

private[puzzle] final class Selector(
    puzzleColl: Coll,
    api: PuzzleApi,
    puzzleIdMin: Int
) {

  import Selector._

  def apply(me: Option[User]): Fu[Puzzle] = {
    lila.mon.puzzle.selector.count()
    me match {
      case None =>
        puzzleColl // this query precisely matches a mongodb partial index
          .find($doc(F.voteNb $gte 50))
          .sort($sort desc F.voteRatio)
          .skip(Random nextInt anonSkipMax)
          .uno[Puzzle]
      case Some(user) =>
        api.head.find(user) flatMap {
          case Some(PuzzleHead(_, Some(c), _)) => api.puzzle.find(c)
          case headOption =>
            newPuzzleForUser(user.perfs.puzzle, headOption) flatMap { next =>
              next.?? { p => api.head.addNew(user, p.id) } inject next
            }
        }
    }
  }.mon(_.puzzle.selector.time) flatten "No puzzles available" addEffect { puzzle =>
    if (puzzle.vote.sum < -1000)
      logger.warn(s"Select #${puzzle.id} vote.sum: ${puzzle.vote.sum} for ${me.fold("Anon")(_.username)} (${me.fold("?")(_.perfs.puzzle.intRating.toString)})")
    else
      lila.mon.puzzle.selector.vote(puzzle.vote.sum)
  }

  private def newPuzzleForUser(perf: Perf, headOption: Option[PuzzleHead]): Fu[Option[Puzzle]] = {
    val rating = perf.intRating atMost 2300 atLeast 900
    val step = toleranceStepFor(rating, perf.nb)
    api.puzzle.cachedLastId.get flatMap { maxId =>
      val lastId = headOption match {
        case Some(PuzzleHead(_, _, l)) if l < maxId - 300 => l
        case _ => puzzleIdMin
      }
      tryRange(
        rating = rating,
        tolerance = step,
        step = step,
        idRange = Range(lastId, lastId + 200)
      )
    }
  }

  private def tryRange(
    rating: Int,
    tolerance: Int,
    step: Int,
    idRange: Range
  ): Fu[Option[Puzzle]] = puzzleColl.find(rangeSelector(
    rating = rating,
    tolerance = tolerance,
    idRange = idRange
  )).sort($sort asc F.id).uno[Puzzle] flatMap {
    case None if (tolerance + step) <= toleranceMax =>
      tryRange(rating, tolerance + step, step, Range(idRange.min, idRange.max + 100))
    case res => fuccess(res)
  }
}

private final object Selector {

  val toleranceMax = 1000

  val anonSkipMax = 5000

  def toleranceStepFor(rating: Int, nbPuzzles: Int) = {
    math.abs(1500 - rating) match {
      case d if d >= 500 => 300
      case d if d >= 300 => 250
      case d => 200
    }
  } * {
    // increase rating tolerance for puzzle blitzers,
    // so they get more puzzles to play
    if (nbPuzzles > 10000) 2
    else if (nbPuzzles > 5000) 3 / 2
    else 1
  }

  def rangeSelector(rating: Int, tolerance: Int, idRange: Range) = $doc(
    F.id $gt idRange.min $lt idRange.max,
    F.rating $gt (rating - tolerance) $lt (rating + tolerance),
    $or(
      F.voteRatio $gt AggregateVote.minRatio,
      F.voteNb $lt AggregateVote.minVotes
    )
  )
}
