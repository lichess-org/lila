package lila.puzzle

import scala.concurrent.duration._
import scala.util.Random

import lila.db.dsl._
import lila.user.User

private[puzzle] final class Selector(
    puzzleColl: Coll,
    api: PuzzleApi,
    puzzleIdMin: Int) {

  private val toleranceMax = 1000

  val anonSkipMax = 5000

  def apply(me: Option[User]): Fu[Puzzle] = {
    lila.mon.puzzle.selector.count()
    me match {
      case None =>
        puzzleColl.find($empty)
          .sort($sort desc "vote.sum")
          .skip(Random nextInt anonSkipMax)
          .uno[Puzzle]
      case Some(user) =>
        api.head.find(user) flatMap {
          case Some(PuzzleHead(_, Some(c), _)) => api.puzzle.find(c)
          case _ =>
            val isLearn = scala.util.Random.nextInt(7) == 0
            val next = if (isLearn) api.learning.nextPuzzle(user) flatMap {
              case None => newPuzzleForUser(user)
              case p    => fuccess(p)
            }
            else newPuzzleForUser(user)
            (next flatMap {
              case Some(p) if isLearn => api.head.addLearning(user, p.id)
              case Some(p)            => api.head.addNew(user, p.id)
              case _                  => fuccess(none)
            }) >> next
        }
    }
  }.mon(_.puzzle.selector.time) flatten "No puzzles available" addEffect { puzzle =>
    lila.mon.puzzle.selector.vote(puzzle.vote.sum)
  }

  private def toleranceStepFor(rating: Int) =
    math.abs(1500 - rating) match {
      case d if d >= 500 => 300
      case d if d >= 300 => 250
      case d             => 200
    }

  private def newPuzzleForUser(user: User): Fu[Option[Puzzle]] = {
    val rating = user.perfs.puzzle.intRating min 2300 max 900
    val step = toleranceStepFor(rating)
    (api.head.find(user) zip api.puzzle.cachedLastId(true)) flatMap {
      case (opHead, maxId) => tryRange(
        rating = rating,
        tolerance = step,
        step = step,
        last = opHead match {
          case Some(PuzzleHead(_, _, l)) if l < maxId - 500 => l
          case _ => puzzleIdMin
        },
        idRange = 200,
        idStep = 100)
    }
  }

  private def tryRange(
    rating: Int,
    tolerance: Int,
    step: Int,
    last: PuzzleId,
    idRange: Int,
    idStep: Int): Fu[Option[Puzzle]] =
    puzzleColl.find($doc(
      Puzzle.BSONFields.id $gt
        last $lt
        (last + idRange),
      Puzzle.BSONFields.rating $gt
        (rating - tolerance) $lt
        (rating + tolerance),
      Puzzle.BSONFields.voteSum $gt -10
    )).uno[Puzzle] flatMap {
      case None if (tolerance + step) <= toleranceMax =>
        tryRange(rating, tolerance + step, step, last, idRange + idStep, idStep)
      case res => fuccess(res)
    }
}
