package lila.puzzle

import scala.util.Random

import reactivemongo.api.QueryOpts
import reactivemongo.bson.{ BSONDocument, BSONInteger }

import lila.db.Types.Coll
import lila.user.User

private[puzzle] final class Selector(puzzleColl: Coll) {

  private val skipMax = 3000
  private val ratingToleranceStep = 100
  private val ratingToleranceMax = 1000

  def apply(me: Option[User]): Fu[Puzzle] = me match {
    case None ⇒ puzzleColl.find(BSONDocument())
      .projection(Puzzle.withoutUsers)
      .options(QueryOpts(skipN = Random.nextInt(skipMax)))
      .cursor[Puzzle]
      .collect[List](1)
      .map(_.headOption) flatten "Can't find a puzzle for anon player!"
    case Some(user) ⇒ tryRange(user, 100)
  }

  private def tryRange(user: User, tolerance: Int): Fu[Puzzle] = puzzleColl.find(BSONDocument(
    Puzzle.BSONFields.users -> BSONDocument("$ne" -> user.id),
    Puzzle.BSONFields.rating -> BSONDocument(
      "$gt" -> BSONInteger(user.perfs.puzzle.intRating + tolerance),
      "$lt" -> BSONInteger(user.perfs.puzzle.intRating + tolerance)
    )
  ), BSONDocument(
    Puzzle.BSONFields.users -> false
  )).projection(Puzzle.withoutUsers)
    .sort(BSONDocument(Puzzle.BSONFields.voteSum -> -1))
    .cursor[Puzzle]
    .collect[List](1)
    .map(_.headOption) flatMap {
      case Some(puzzle) => fuccess(puzzle)
      case None => if ((tolerance + ratingToleranceStep) <= ratingToleranceMax)
        tryRange(user, tolerance + ratingToleranceStep)
        else fufail(s"Can't find a puzzle for user $user!")
    }

}
