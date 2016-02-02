package lila.challenge

import org.joda.time.DateTime
import reactivemongo.bson.{ BSONDocument, BSONInteger, BSONRegex, BSONArray, BSONBoolean }
import scala.concurrent.duration._

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.Types.Coll
import lila.user.{ User, UserRepo }

private final class ChallengeRepo(coll: Coll, maxPerUser: Int) {

  import BSONHandlers._
  import Challenge._

  def byId(id: Challenge.ID) = coll.find(selectId(id)).one[Challenge]

  def exists(id: Challenge.ID) = coll.count(selectId(id).some).map(0<)

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

  def countCreatedByDestId(userId: String): Fu[Int] =
    coll.count(Some(selectCreated ++ BSONDocument("destUser.id" -> userId)))

  def unseenSince(date: DateTime, max: Int): Fu[List[Challenge]] =
    coll.find(selectCreated ++ selectClock ++ BSONDocument(
      "seenAt" -> BSONDocument("$lt" -> date)
    )).cursor[Challenge]().collect[List](max)

  def setSeenAgain(id: Challenge.ID) = coll.update(
    selectId(id),
    BSONDocument("$set" -> BSONDocument(
      "status" -> Status.Created.id,
      "seenAt" -> DateTime.now))
  ).void

  def setSeen(id: Challenge.ID) = coll.update(
    selectId(id),
    BSONDocument("$set" -> BSONDocument("seenAt" -> DateTime.now))
  ).void

  def offline(challenge: Challenge) = setStatus(challenge, Status.Offline)
  def cancel(challenge: Challenge) = setStatus(challenge, Status.Canceled)
  def decline(challenge: Challenge) = setStatus(challenge, Status.Declined)
  def accept(challenge: Challenge) = setStatus(challenge, Status.Accepted)

  def statusById(id: Challenge.ID) = coll.find(
    selectId(id),
    BSONDocument("status" -> true, "_id" -> false)
  ).one[BSONDocument].map { _.flatMap(_.getAs[Status]("status")) }

  private def setStatus(challenge: Challenge, status: Status) = coll.update(
    selectCreated ++ selectId(challenge.id),
    BSONDocument("$set" -> BSONDocument("status" -> status.id))
  ).void

  private def remove(challenge: Challenge) =
    coll.remove(selectId(challenge.id)).void

  private def selectId(id: Challenge.ID) = BSONDocument("_id" -> id)
  private val selectCreated = BSONDocument("status" -> Status.Created.id)
  private val selectClock = BSONDocument("timeControl.l" -> BSONDocument("$exists" -> true))
}

