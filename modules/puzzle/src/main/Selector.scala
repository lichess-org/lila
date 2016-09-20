package lila.puzzle

import scala.concurrent.duration._
import scala.util.Random

import lila.db.dsl._
import lila.user.User

private[puzzle] final class Selector(
    puzzleColl: Coll,
    api: PuzzleApi,
    anonMinRating: Int,
    maxAttempts: Int) {

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

  def apply(me: Option[User], difficulty: Int): Fu[Option[Puzzle]] = {
    lila.mon.puzzle.selector.count()
    val isMate = scala.util.Random.nextBoolean
    me match {
      case None =>
        puzzleColl.find(popularSelector(isMate) ++ mateSelector(isMate))
          .skip(Random nextInt anonSkipMax)
          .uno[Puzzle]
      case Some(user) if user.perfs.puzzle.nb >= maxAttempts => fuccess(none)
      case Some(user) =>
        val rating = user.perfs.puzzle.intRating min 2300 max 900
        val step = toleranceStepFor(rating)
        api.attempt.playedIds(user) flatMap { ids =>
          tryRange(rating, step, step, difficultyDecay(difficulty), ids, isMate)
        }
    }
  }.mon(_.puzzle.selector.time) addEffect {
    _ foreach { puzzle =>
      lila.mon.puzzle.selector.vote(puzzle.vote.sum)
    }
  }

  private def toleranceStepFor(rating: Int) =
    math.abs(1500 - rating) match {
      case d if d >= 500 => 300
      case d if d >= 300 => 250
      case d             => 200
    }

  private def tryRange(rating: Int, tolerance: Int, step: Int, decay: Int, ids: Barr, isMate: Boolean): Fu[Option[Puzzle]] =
    puzzleColl.find(mateSelector(isMate) ++ $doc(
      Puzzle.BSONFields.id -> $doc("$nin" -> ids),
      Puzzle.BSONFields.rating $gt
        (rating - tolerance + decay) $lt
        (rating + tolerance + decay)
    )).sort($sort desc Puzzle.BSONFields.voteSum)
      .uno[Puzzle] flatMap {
        case None if (tolerance + step) <= toleranceMax =>
          tryRange(rating, tolerance + step, step, decay, ids, isMate)
        case res => fuccess(res)
      }
}
