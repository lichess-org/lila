package lila.challenge

import reactivemongo.bson.{ BSONDocument, BSONInteger, BSONRegex, BSONArray, BSONBoolean }
import scala.concurrent.duration._
import org.joda.time.DateTime

import lila.db.Types.Coll
import lila.db.BSON.BSONJodaDateTimeHandler 
import lila.user.{ User, UserRepo }

private final class ChallengeRepo(coll: Coll, maxPerUser: Int) {

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
      .sort(BSONDocument("createdAt" -> 1))
      .cursor[Challenge]().collect[List]()

  def createdByDestId(userId: String): Fu[List[Challenge]] =
    coll.find(selectCreated ++ BSONDocument("destUser.id" -> userId))
      .sort(BSONDocument("createdAt" -> 1))
      .cursor[Challenge]().collect[List]()

  def createdByDestIds(userIds: List[String]): Fu[Map[String, Seq[Challenge]]] =
    coll.find(selectCreated ++ BSONDocument("destUser.id" -> BSONDocument("$in" -> userIds)))
      .sort(BSONDocument("createdAt" -> 1))
      .cursor[Challenge]().collect[Stream]().map { _.groupBy(~_.destUserId) }

  def countCreatedByDestId(userId: String): Fu[Int] =
    coll.count(Some(selectCreated ++ BSONDocument("destUser.id" -> userId)))

  def unseenSince(date: DateTime, max: Int): Fu[List[Challenge]] =
    coll.find(selectCreated ++ selectClock ++ BSONDocument(
      "seenAt" -> BSONDocument("$lt" -> date)
    )).cursor[Challenge]().collect[List](max)

  def cancel(challenge: Challenge) = setStatus(challenge, Status.Canceled)
  def abandon(challenge: Challenge) = setStatus(challenge, Status.Abandoned)
  def decline(challenge: Challenge) = setStatus(challenge, Status.Declined)
  def accept(challenge: Challenge) = setStatus(challenge, Status.Accepted)

  private def setStatus(challenge: Challenge, status: Status) = coll.update(
    selectCreated ++ BSONDocument("_id" -> challenge.id),
    BSONDocument("$set" -> BSONDocument("status" -> status.id))
  ).void

  private def remove(challenge: Challenge) =
    coll.remove(BSONDocument("_id" -> challenge.id)).void

  private val selectCreated = BSONDocument("status" -> Status.Created.id)

  private val selectClock = BSONDocument("timeControl.l" -> BSONDocument("$exists" -> true))
}

