package lila.setup

import org.joda.time.DateTime
import reactivemongo.bson.{ BSONDocument, BSONInteger, BSONRegex, BSONArray, BSONBoolean }
import reactivemongo.core.commands._
import scala.concurrent.duration._

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.Types.Coll
import lila.user.{ User, UserRepo }

final class ChallengeApi(
    coll: Coll,
    maxPerUser: Int) {

  def findByChallengerId(userId: String): Fu[List[Challenge]] =
    coll.find(BSONDocument("challenger.id" -> userId))
      .sort(BSONDocument("createdAt" -> -1))
      .cursor[Challenge]().collect[List]()

  def insert(challenge: Challenge) =
    coll.insert(challenge) >> findByChallengerId(challenge.challenger.id).flatMap {
      case challenges if challenges.size <= maxPerUser => funit
      case challenges                                  => challenges.drop(maxPerUser).map(remove).sequenceFu
    }

  def findByDestId(userId: String): Fu[List[Challenge]] =
    coll.find(BSONDocument("destUserId" -> userId))
      .sort(BSONDocument("createdAt" -> -1))
      .cursor[Challenge]().collect[List]()

  def findByDestIds(userIds: List[String]): Fu[Map[String, Seq[Challenge]]] =
    coll.find(BSONDocument("destUserId" -> BSONDocument("$in" -> userIds)))
      .sort(BSONDocument("createdAt" -> -1))
      .cursor[Challenge]().collect[List]().map { challenges =>
        challenges.groupBy(_.destUserId)
      }

  def remove(challenge: Challenge) =
    coll.remove(BSONDocument("_id" -> challenge.id)).void
}
