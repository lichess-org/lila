package lila.challenge

import reactivemongo.bson.{ BSONDocument, BSONInteger, BSONRegex, BSONArray, BSONBoolean }
import scala.concurrent.duration._

import lila.db.Types.Coll
import lila.user.{ User, UserRepo }

final class ChallengeApi(
    coll: Coll,
    maxPerUser: Int) {

  import BSONHandlers._
  import Challenge._

  def byId(id: String) = coll.find(BSONDocument("_id" -> id)).one[Challenge]

  def insert(c: Challenge): Funit =
    coll.insert(c) >> c.challenger.right.toOption.?? { challenger =>
      createdByChallengerId(challenger.id).flatMap {
        case challenges if challenges.size <= maxPerUser => funit
        case challenges                                  => challenges.drop(maxPerUser).map(remove).sequenceFu.void
      }
    }

  def createdByChallengerId(userId: String): Fu[List[Challenge]] =
    coll.find(selectCreated ++ BSONDocument("challenger.id" -> userId))
      .sort(BSONDocument("createdAt" -> -1))
      .cursor[Challenge]().collect[List]()

  def createdByDestId(userId: String): Fu[List[Challenge]] =
    coll.find(selectCreated ++ BSONDocument("destUserId" -> userId))
      .sort(BSONDocument("createdAt" -> -1))
      .cursor[Challenge]().collect[List]()

  def createdByDestIds(userIds: List[String]): Fu[Map[String, Seq[Challenge]]] =
    coll.find(selectCreated ++ BSONDocument("destUserId" -> BSONDocument("$in" -> userIds)))
      .sort(BSONDocument("createdAt" -> -1))
      .cursor[Challenge]().collect[Stream]().map { _.groupBy(~_.destUserId) }

  def remove(challenge: Challenge) =
    coll.remove(BSONDocument("_id" -> challenge.id)).void

  def decline(challenge: Challenge) = coll.update(
    selectCreated ++ BSONDocument("_id" -> challenge.id),
    BSONDocument("$set" -> BSONDocument("state" -> State.Declined.id))
  ).void

  private val selectCreated = BSONDocument("state" -> State.Created.id)
}
