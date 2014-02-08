package lila.puzzle

import scala.concurrent.duration._
import scala.util.Random

import reactivemongo.api.QueryOpts
import reactivemongo.bson.{ BSONDocument, BSONInteger }
import reactivemongo.core.commands.Count

import lila.db.Types.Coll
import lila.user.User

private[puzzle] final class Selector(puzzleColl: Coll) {

  private val count = lila.memo.AsyncCache.single[Int](
    f = puzzleColl.db command Count(puzzleColl.name, none),
    timeToLive = 1 hour)

  private val ratingToleranceStep = 100
  private val ratingToleranceMax = 1000

  def apply(me: Option[User]): Fu[Puzzle] = me match {
    case None ⇒ count(true) map (_ - 1) flatMap { skipMax ⇒
      puzzleColl.find(BSONDocument())
        .projection(Puzzle.withoutUsers)
        .options(QueryOpts(skipN = Random.nextInt(skipMax)))
        .cursor[Puzzle]
        .collect[List](1)
        .map(_.headOption) flatten "Can't find a puzzle for anon player!"
    }
    case Some(user) ⇒ tryRange(user, 100)
  }

  private def tryRange(user: User, tolerance: Int): Fu[Puzzle] = puzzleColl.find(BSONDocument(
    Puzzle.BSONFields.users -> BSONDocument("$ne" -> user.id),
    Puzzle.BSONFields.rating -> BSONDocument(
      "$gt" -> BSONInteger(user.perfs.puzzle.intRating - tolerance),
      "$lt" -> BSONInteger(user.perfs.puzzle.intRating + tolerance)
    )
  ), BSONDocument(
    Puzzle.BSONFields.users -> false
  )).projection(Puzzle.withoutUsers)
    .sort(BSONDocument(Puzzle.BSONFields.voteSum -> -1))
    .cursor[Puzzle]
    .collect[List](1)
    .map(_.headOption) flatMap {
      case Some(puzzle) ⇒ fuccess(puzzle)
      case None ⇒ if ((tolerance + ratingToleranceStep) <= ratingToleranceMax)
        tryRange(user, tolerance + ratingToleranceStep)
      else fufail(s"Can't find a puzzle for user $user!")
    }

}
