package lila.puzzle

import scala.util.Random

import lila.db.dsl._
import lila.rating.Perf
import lila.user.User
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
      // anon
      case None => puzzleColl // this query precisely matches a mongodb partial index
        .find($doc(F.voteNb $gte 50))
        .sort($sort desc F.voteRatio)
        .skip(Random nextInt anonSkipMax)
        .uno[Puzzle]
      // user
      case Some(user) => api.head find user flatMap {
        // new player
        case None => api.puzzle find puzzleIdMin flatMap { puzzleOption =>
          puzzleOption ?? { p => api.head.addNew(user, p.id) } inject puzzleOption
        }
        // current puzzle
        case Some(PuzzleHead(_, Some(current), _)) => api.puzzle find current
        // find new based on last
        case Some(PuzzleHead(_, _, last)) => newPuzzleForUser(user, last) flatMap {
          // user played all puzzles. Reset rounds and start anew.
          case None => api.puzzle.cachedLastId.get flatMap { maxId =>
            (last > maxId - 1000) ?? {
              api.round.reset(user) >> api.puzzle.find(puzzleIdMin)
            }
          }
          case Some(found) => fuccess(found.some)
        } flatMap { puzzleOption =>
          puzzleOption ?? { p => api.head.addNew(user, p.id) } inject puzzleOption
        }
      }
    }
  }.mon(_.puzzle.selector.time) flattenWith NoPuzzlesAvailableException addEffect { puzzle =>
    if (puzzle.vote.sum < -1000)
      logger.info(s"Select #${puzzle.id} vote.sum: ${puzzle.vote.sum} for ${me.fold("Anon")(_.username)} (${me.fold("?")(_.perfs.puzzle.intRating.toString)})")
    else
      lila.mon.puzzle.selector.vote(puzzle.vote.sum)
  }

  private def newPuzzleForUser(user: User, lastPlayed: PuzzleId): Fu[Option[Puzzle]] = {
    val rating = user.perfs.puzzle.intRating atMost 2300 atLeast 900
    val step = toleranceStepFor(rating, user.perfs.puzzle.nb)
    tryRange(
      rating = rating,
      tolerance = step,
      step = step,
      idRange = Range(lastPlayed, lastPlayed + 200)
    )
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

  case object NoPuzzlesAvailableException extends lila.base.LilaException {
    val message = "No puzzles available"
  }

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
