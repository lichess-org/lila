package lila.puzzle

import scala.concurrent.duration._
import scala.util.Random

import reactivemongo.api.QueryOpts
import reactivemongo.bson.{ BSONDocument, BSONInteger, BSONArray }

import lila.db.Types.Coll
import lila.user.User

private[puzzle] final class Selector(
    puzzleColl: Coll,
    api: PuzzleApi,
    anonMinRating: Int,
    maxAttempts: Int) {

  private def popularSelector(mate: Boolean) = BSONDocument(
    Puzzle.BSONFields.voteSum -> BSONDocument("$gt" -> BSONInteger(mate.fold(anonMinRating, 0))))

  private def mateSelector(mate: Boolean) = BSONDocument("mate" -> mate)

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
          .options(QueryOpts(skipN = Random nextInt anonSkipMax))
          .one[Puzzle]
      case Some(user) if user.perfs.puzzle.nb > maxAttempts => fuccess(none)
      case Some(user) =>
        val rating = user.perfs.puzzle.intRating min 2300 max 900
        val step = toleranceStepFor(rating)
        api.attempt.playedIds(user, maxAttempts) flatMap { ids =>
          tryRange(rating, step, step, difficultyDecay(difficulty), ids, isMate)
        }
    }
  }.chronometer.mon(_.puzzle.selector.time).result

  private def toleranceStepFor(rating: Int) =
    math.abs(1500 - rating) match {
      case d if d >= 500 => 300
      case d if d >= 300 => 250
      case d             => 200
    }

  private def tryRange(rating: Int, tolerance: Int, step: Int, decay: Int, ids: BSONArray, isMate: Boolean): Fu[Option[Puzzle]] =
    puzzleColl.find(mateSelector(isMate) ++ BSONDocument(
      Puzzle.BSONFields.id -> BSONDocument("$nin" -> ids),
      Puzzle.BSONFields.rating -> BSONDocument(
        "$gt" -> BSONInteger(rating - tolerance + decay),
        "$lt" -> BSONInteger(rating + tolerance + decay)
      )
    )).sort(BSONDocument(Puzzle.BSONFields.voteSum -> -1))
      .one[Puzzle] flatMap {
        case None if (tolerance + step) <= toleranceMax =>
          tryRange(rating, tolerance + step, step, decay, ids, isMate)
        case res => fuccess(res)
      }
}
