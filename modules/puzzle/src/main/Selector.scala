package lila.puzzle

import scala.concurrent.duration._
import scala.util.Random

import lila.db.dsl._
import lila.user.User

private[puzzle] final class Selector(
    puzzleColl: Coll,
    api: PuzzleApi,
    anonMinRating: Int) {

  private def popularSelector(mate: Boolean) = $doc(
    Puzzle.BSONFields.voteSum $gt mate.fold(anonMinRating, 0))

  private def mateSelector(mate: Boolean) = $doc("mate" -> mate)

  private def difficultyDecay(difficulty: Int) = difficulty match {
    case 1 => -200
    case 3 => +200
    case _ => 0
  }

  private val toleranceMax = 1000

  val anonSkipMax = 5000

  def apply(me: Option[User], difficulty: Int): Fu[Puzzle] = {
    lila.mon.puzzle.selector.count()
    val isMate = scala.util.Random.nextBoolean
    me match {
      case None =>
        puzzleColl.find(popularSelector(isMate) ++ mateSelector(isMate))
          .skip(Random nextInt anonSkipMax)
          .uno[Puzzle]
      case Some(user) =>
        api.head.find(user) flatMap {
          case Some(PuzzleHead(_, Some(c), _)) => api.puzzle.find(c)
          case _ =>
            val isLearn = scala.util.Random.nextInt(5) == 0
            val next = if (isLearn) api.learning.nextPuzzle(user) flatMap {
                case None => newPuzzleForUser(user, isMate, difficulty)
                case p => fuccess(p)
              }
              else newPuzzleForUser(user, isMate, difficulty)
            (next flatMap {
              case Some(p) if isLearn => api.head.addLearning(user, p.id)
              case Some(p)            => api.head.addNew(user, p.id)
              case _ => fuccess(none)
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

  private def newPuzzleForUser(user: User, isMate: Boolean, difficulty: Int): Fu[Option[Puzzle]] = {
    val rating = user.perfs.puzzle.intRating min 2300 max 900
    val step = toleranceStepFor(rating)
    (api.head.find(user) zip api.puzzle.lastId) flatMap {
      case (opHead, maxId) => tryRange(rating, step, step, difficultyDecay(difficulty), opHead match {
          case Some(PuzzleHead(_, _, l)) if l < maxId - 500 => l
          case _ => 0
        }, 100, 100, isMate)
    }
  }

  private def tryRange(rating: Int, tolerance: Int, step: Int, decay: Int, last: PuzzleId, idRange: Int, idStep: Int, isMate: Boolean): Fu[Option[Puzzle]] =
    puzzleColl.find(mateSelector(isMate) ++ $doc(
      Puzzle.BSONFields.id $gt
        last $lt
        (last + idRange),
      Puzzle.BSONFields.rating $gt
        (rating - tolerance + decay) $lt
        (rating + tolerance + decay)
    )).sort($sort desc Puzzle.BSONFields.voteSum)
      .uno[Puzzle] flatMap {
        case None if (tolerance + step) <= toleranceMax =>
          tryRange(rating, tolerance + step, step, decay, last, idRange + idStep, idStep, isMate)
        case res => fuccess(res)
      }
}
