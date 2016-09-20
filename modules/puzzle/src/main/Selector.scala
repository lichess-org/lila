package lila.puzzle

import scala.concurrent.duration._
import scala.util.Random

import lila.db.dsl._
import lila.user.User

private[puzzle] final class Selector(
    puzzleColl: Coll,
    api: PuzzleApi,
    anonMinRating: Int,
    puzzleIdMin: Int) {

  private def popularSelector = $doc(Puzzle.BSONFields.voteSum $gt 0)

  private def difficultyDecay(difficulty: Int) = difficulty match {
    case 1 => -200
    case 3 => +200
    case _ => 0
  }

  private val toleranceMax = 1000

  val anonSkipMax = 5000

  def apply(me: Option[User], difficulty: Int): Fu[Puzzle] = {
    lila.mon.puzzle.selector.count()
    me match {
      case None =>
        puzzleColl.find(popularSelector)
          .skip(Random nextInt anonSkipMax)
          .uno[Puzzle]
      case Some(user) =>
        api.head.find(user) flatMap {
          case Some(PuzzleHead(_, Some(c), _)) => api.puzzle.find(c)
          case _ =>
            val isLearn = scala.util.Random.nextInt(5) == 0
            val next = if (isLearn) api.learning.nextPuzzle(user) flatMap {
              case None => newPuzzleForUser(user, difficulty)
              case p    => fuccess(p)
            }
            else newPuzzleForUser(user, difficulty)
            (next flatMap {
              case Some(p) if isLearn => api.head.addLearning(user, p.id)
              case Some(p)            => api.head.addNew(user, p.id)
              case _                  => fuccess(none)
            }) >> next
        }
    }
  }.mon(_.puzzle.selector.time) flatten "No puzzles available"

  private def toleranceStepFor(rating: Int) =
    math.abs(1500 - rating) match {
      case d if d >= 500 => 300
      case d if d >= 300 => 250
      case d             => 200
    }

  private def newPuzzleForUser(user: User, difficulty: Int): Fu[Option[Puzzle]] = {
    val rating = user.perfs.puzzle.intRating min 2300 max 900
    val step = toleranceStepFor(rating)
    (api.head.find(user) zip api.puzzle.lastId) flatMap {
      case (opHead, maxId) => tryRange(
        rating = rating,
        tolerance = step,
        step = step,
        decay = difficultyDecay(difficulty),
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
    decay: Int,
    last: PuzzleId,
    idRange: Int,
    idStep: Int): Fu[Option[Puzzle]] =
    puzzleColl.find($doc(
      Puzzle.BSONFields.id $gt
        last $lt
        (last + idRange),
      Puzzle.BSONFields.rating $gt
        (rating - tolerance + decay) $lt
        (rating + tolerance + decay),
      Puzzle.BSONFields.voteSum $gt -10
    )).uno[Puzzle] flatMap {
      case None if (tolerance + step) <= toleranceMax =>
        tryRange(rating, tolerance + step, step, decay, last, idRange + idStep, idStep)
      case res => fuccess(res)
    }
}
