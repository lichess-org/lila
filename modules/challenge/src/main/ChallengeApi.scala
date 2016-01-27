package lila.challenge

import reactivemongo.bson.{ BSONDocument, BSONInteger, BSONRegex, BSONArray, BSONBoolean }
import scala.concurrent.duration._

import lila.db.Types.Coll
import lila.user.{ User, UserRepo }

final class ChallengeApi(
    coll: Coll,
    maxPerUser: Int) {

  import BSONHandlers._

  def findByChallengerId(userId: String): Fu[List[Challenge]] =
    coll.find(BSONDocument("challenger.id" -> userId))
      .sort(BSONDocument("createdAt" -> -1))
      .cursor[Challenge]().collect[List]()

  def insert(c: Challenge) =
    coll.insert(c) >> c.challenger.right.toOption.?? { challenger =>
      findByChallengerId(challenger.id).flatMap {
        case challenges if challenges.size <= maxPerUser => funit
        case challenges                                  => challenges.drop(maxPerUser).map(remove).sequenceFu.void
      }
    }

  def findByDestId(userId: String): Fu[List[Challenge]] =
    coll.find(BSONDocument("destUserId" -> userId))
      .sort(BSONDocument("createdAt" -> -1))
      .cursor[Challenge]().collect[List]()

  def findByDestIds(userIds: List[String]): Fu[Map[String, Seq[Challenge]]] =
    coll.find(BSONDocument("destUserId" -> BSONDocument("$in" -> userIds)))
      .sort(BSONDocument("createdAt" -> -1))
      .cursor[Challenge]().collect[Stream]().map { _.groupBy(~_.destUserId) }

  def remove(challenge: Challenge) =
    coll.remove(BSONDocument("_id" -> challenge.id)).void
}
