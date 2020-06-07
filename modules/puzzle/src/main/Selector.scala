package lidraughts.puzzle

import scala.util.Random

import lidraughts.db.dsl._
import lidraughts.user.User
import draughts.variant.{ Variant, Standard, Frisian, Russian }
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
      case None => anonPuzzle(variant) flatMap {
        case p @ Some(_) => fuccess(p)
        case _ =>
          logger.warn(s"Retry ${variant.key} puzzle for Anon")
          anonPuzzle(variant)
      }
      case Some(user) => api.head.find(user, variant) flatMap {
        case head @ Some(PuzzleHead(_, Some(c), _)) =>
          api.puzzle.find(c, variant) flatMap {
            case p @ Some(_) => fuccess(p)
            case _ =>
              logger.warn(s"${variant.key} puzzle $c not found for ${user.username}")
              retryNewPuzzleForUser(user, variant, head)
          }
        case headOption =>
          retryNewPuzzleForUser(user, variant, headOption)
      }
    }
  }.mon(_.puzzle.selector.time) flatten s"No ${variant.key} puzzle available" addEffect { puzzle =>
    if (puzzle.vote.sum < -1000)
      logger.info(s"Select #${puzzle.id} vote.sum: ${puzzle.vote.sum} for ${me.fold("Anon")(_.username)} (${me.fold("?")(_.perfs.puzzle(variant).intRating.toString)})")
    else
      lidraughts.mon.puzzle.selector.vote(puzzle.vote.sum)
  }

  private def anonPuzzle(variant: Variant) =
    puzzleColl(variant) // FIXME: this query precisely matches a mongodb partial index
      .find($doc(F.voteNb $gte 1)) //original 50
      .sort($sort desc F.voteRatio)
      .skip(Random nextInt anonSkipMax(variant))
      .uno[Puzzle]

  private def retryNewPuzzleForUser(user: User, variant: Variant, headOption: Option[PuzzleHead]): Fu[Option[Puzzle]] =
    newPuzzleForUser(user, variant, headOption) flatMap {
      case Some(p) => api.head.addNew(user, p.id, variant) inject p.some
      case _ =>
        logger.warn(s"Retry ${variant.key} puzzle for ${user.username} @ $headOption")
        newPuzzleForUser(user, variant, none) flatMap { next =>
          next.?? { p => api.head.addNew(user, p.id, variant) } inject next
        }
    }

  private def newPuzzleForUser(user: User, variant: Variant, headOption: Option[PuzzleHead]): Fu[Option[Puzzle]] = {
    val perf = user.perfs.puzzle(variant)
    val rating = perf.intRating min 2300 max 900
    val step = toleranceStepFor(rating, perf.nb)
    val tolerance = step * {
      // increase rating tolerance for puzzle blitzers,
      // so they get more puzzles to play
      if (perf.nb > 3000) 2
      else if (perf.nb > 1500) 3 / 2
      else 1
    }
    val skipMax = if (variant.standard) 100 else 50
    val idStep = if (variant.standard) 50 else 20
    api.puzzle.cachedLastId(variant).get flatMap { maxId =>
      val lastId = headOption match {
        case Some(PuzzleHead(_, _, l)) if l < maxId - skipMax => l //original - 500
        case _ => puzzleIdMin
      }
      tryRange(
        variant = variant,
        rating = rating,
        tolerance = tolerance,
        step = step,
        idRange = Range(lastId, lastId + idStep), //original + 200
        idStep = idStep / 2
      )
    }
  }

  private def tryRange(
    variant: Variant,
    rating: Int,
    tolerance: Int,
    step: Int,
    idRange: Range,
    idStep: Int
  ): Fu[Option[Puzzle]] = puzzleColl(variant).find(rangeSelector(
    rating = rating,
    tolerance = tolerance,
    idRange = idRange
  )).sort($sort asc F.id).uno[Puzzle] flatMap {
    case None if (tolerance + step) <= toleranceMax =>
      tryRange(variant, rating, tolerance + step, step, Range(idRange.min, idRange.max + idStep), idStep) //original + 100
    case res => fuccess(res)
  }
}

private final object Selector {

  val toleranceMax = 1500

  val anonSkipMax: Map[Variant, Int] = Map(Standard -> 500, Frisian -> 250, Russian -> 250)

  def toleranceStepFor(rating: Int, nbPuzzles: Int) = {
    math.abs(1500 - rating) match {
      case d if d >= 500 => 300
      case d if d >= 300 => 250
      case d => 200
    }
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
