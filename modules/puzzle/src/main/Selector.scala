package lidraughts.puzzle

import scala.util.Random

import lidraughts.db.dsl._
import lidraughts.user.User
import draughts.variant.Variant
import Puzzle.{ BSONFields => F }

private[puzzle] final class Selector(
    puzzleColl: Map[Variant, Coll],
    api: PuzzleApi,
    puzzleIdMin: Int
) {

  import Selector._

  def apply(me: Option[User], variant: Variant): Fu[Puzzle] = {
    lidraughts.mon.puzzle.selector.count()
    me match {
      case None =>
        puzzleColl(variant) // this query precisely matches a mongodb partial index
          .find($doc(F.voteNb $gte 1)) //original 50
          .sort($sort desc F.voteRatio)
          .skip(Random nextInt anonSkipMax)
          .uno[Puzzle]
      case Some(user) =>
        api.head.find(user, variant) flatMap {
          case Some(PuzzleHead(_, Some(c), _)) => api.puzzle.find(c, variant)
          case headOption =>
            newPuzzleForUser(user, variant, headOption) flatMap { next =>
              next.?? { p => api.head.addNew(user, p.id, variant) } inject next
            }
        }
    }
  }.mon(_.puzzle.selector.time) flatten "No puzzles available" addEffect { puzzle =>
    if (puzzle.vote.sum < -1000)
      logger.warn(s"Select #${puzzle.id} vote.sum: ${puzzle.vote.sum} for ${me.fold("Anon")(_.username)} (${me.fold("?")(_.perfs.puzzle(variant).intRating.toString)})")
    else
      lidraughts.mon.puzzle.selector.vote(puzzle.vote.sum)
  }

  private def newPuzzleForUser(user: User, variant: Variant, headOption: Option[PuzzleHead]): Fu[Option[Puzzle]] = {
    val rating = user.perfs.puzzle(variant).intRating min 2300 max 900
    val step = toleranceStepFor(rating)
    api.puzzle.cachedLastId(variant).get flatMap { maxId =>
      val lastId = headOption match {
        case Some(PuzzleHead(_, _, l)) if l < maxId - 80 => l //original - 500
        case _ => puzzleIdMin
      }
      tryRange(
        variant = variant,
        rating = rating,
        tolerance = step,
        step = step,
        idRange = Range(lastId, lastId + 40) //original + 200
      )
    }
  }

  private def tryRange(
    variant: Variant,
    rating: Int,
    tolerance: Int,
    step: Int,
    idRange: Range
  ): Fu[Option[Puzzle]] = puzzleColl(variant).find(rangeSelector(
    rating = rating,
    tolerance = tolerance,
    idRange = idRange
  )).uno[Puzzle] flatMap {
    case None if (tolerance + step) <= toleranceMax =>
      tryRange(variant, rating, tolerance + step, step,
        idRange = Range(idRange.min, idRange.max + 20)) //original + 100
    case res => fuccess(res)
  }
}

private final object Selector {

  val toleranceMax = 1000

  val anonSkipMax = 500 //original 5000

  def toleranceStepFor(rating: Int) =
    math.abs(1500 - rating) match {
      case d if d >= 500 => 300
      case d if d >= 300 => 250
      case d => 200
    }

  def rangeSelector(rating: Int, tolerance: Int, idRange: Range) = $doc(
    F.id $gt
      idRange.min $lt
      idRange.max,
    F.rating $gt
      (rating - tolerance) $lt
      (rating + tolerance),
    $or(
      F.voteRatio $gt AggregateVote.minRatio,
      F.voteNb $lt AggregateVote.minVotes
    )
  )
}
