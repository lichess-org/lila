package lila.puzzle

import scala.concurrent.duration._
import scala.util.Random

import reactivemongo.api.QueryOpts
import reactivemongo.bson.{ BSONDocument, BSONInteger }
import reactivemongo.core.commands.Count

import lila.db.Types.Coll
import lila.user.User

private[puzzle] final class Selector(puzzleColl: Coll) {

  private val anonMinRating = 30
  private val ratingToleranceStep = 160
  private val ratingToleranceMax = 1000

  private val popularSelector = BSONDocument(
    Puzzle.BSONFields.voteSum -> BSONDocument("$gt" -> BSONInteger(anonMinRating))
  )

  private val popularCount = lila.memo.AsyncCache.single[Int](
    f = puzzleColl.db command Count(puzzleColl.name, popularSelector.some),
    timeToLive = 1 hour)

  private def difficultyDecay(difficulty: Int) = difficulty match {
    case 1 => -200
    case 3 => +200
    case _ => 0
  }

  def apply(me: Option[User], difficulty: Int): Fu[Puzzle] = me match {
    case None => popularCount(true) map (_ - 1) flatMap { skipMax =>
      puzzleColl.find(popularSelector)
        .projection(Puzzle.withoutUsers)
        .options(QueryOpts(skipN = Random nextInt skipMax))
        .one[Puzzle] flatten "Can't find a puzzle for anon player!"
    }
    case Some(user) => tryRange(user, ratingToleranceStep, difficultyDecay(difficulty))
  }

  private def tryRange(user: User, tolerance: Int, decay: Int): Fu[Puzzle] = puzzleColl.find(BSONDocument(
    Puzzle.BSONFields.users -> BSONDocument("$ne" -> user.id),
    Puzzle.BSONFields.rating -> BSONDocument(
      "$gt" -> BSONInteger(user.perfs.puzzle.intRating - tolerance + decay),
      "$lt" -> BSONInteger(user.perfs.puzzle.intRating + tolerance + decay)
    )
  ), BSONDocument(
    Puzzle.BSONFields.users -> false
  )).projection(Puzzle.withoutUsers)
    .sort(BSONDocument(Puzzle.BSONFields.voteSum -> -1))
    .one[Puzzle] flatMap {
      case Some(puzzle) => fuccess(puzzle)
      case None => if ((tolerance + ratingToleranceStep) <= ratingToleranceMax)
        tryRange(user, tolerance + ratingToleranceStep, decay)
      else fufail(s"Can't find a puzzle for user $user!")
    }
}
