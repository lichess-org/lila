package lila.puzzle

import scala.concurrent.duration._
import scala.util.Random

import reactivemongo.api.QueryOpts
import reactivemongo.bson.{ BSONDocument, BSONInteger, BSONArray }
import reactivemongo.core.commands.Count

import lila.db.Types.Coll
import lila.user.User

private[puzzle] final class Selector(
    puzzleColl: Coll,
    api: PuzzleApi,
    anonMinRating: Int,
    toleranceStep: Int,
    toleranceMax: Int,
    modulo: Int) {

  private val popularSelector = BSONDocument(
    Puzzle.BSONFields.voteSum -> BSONDocument("$gt" -> BSONInteger(anonMinRating)))

  private def mateSelector(mate: Boolean) = BSONDocument("mate" -> mate)

  private lazy val popularCount = puzzleColl.db command Count(puzzleColl.name, popularSelector.some)

  private def difficultyDecay(difficulty: Int) = difficulty match {
    case 1 => -200
    case 3 => +200
    case _ => 0
  }

  def apply(me: Option[User], difficulty: Int): Fu[Puzzle] = {
    val isMate = scala.util.Random.nextBoolean
    me match {
      case None => popularCount map (_ - 1) flatMap { skipMax =>
        puzzleColl.find(popularSelector ++ mateSelector(isMate))
          .options(QueryOpts(skipN = Random nextInt skipMax))
          .one[Puzzle] flatten "Can't find a puzzle for anon player!"
      }
      case Some(user) => api.attempt.playedIds(user, modulo) flatMap { ids =>
        tryRange(user, toleranceStep, difficultyDecay(difficulty), ids, isMate)
      }
    }
  }

  private def tryRange(user: User, tolerance: Int, decay: Int, ids: BSONArray, isMate: Boolean): Fu[Puzzle] =
    puzzleColl.find(mateSelector(isMate) ++ BSONDocument(
      Puzzle.BSONFields.id -> BSONDocument("$nin" -> ids),
      Puzzle.BSONFields.rating -> BSONDocument(
        "$gt" -> BSONInteger(user.perfs.puzzle.intRating - tolerance + decay),
        "$lt" -> BSONInteger(user.perfs.puzzle.intRating + tolerance + decay)
      )
    )).sort(BSONDocument(Puzzle.BSONFields.voteSum -> -1))
      .one[Puzzle] flatMap {
        case Some(puzzle) => fuccess(puzzle)
        case None => if ((tolerance + toleranceStep) <= toleranceMax)
          tryRange(user, tolerance + toleranceStep, decay, ids, isMate)
        else fufail(s"Can't find a puzzle for user $user!")
      }
}
