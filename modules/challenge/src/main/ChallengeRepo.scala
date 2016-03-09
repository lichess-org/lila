package lila.challenge

import org.joda.time.DateTime
import reactivemongo.bson.{ BSONDocument, BSONInteger, BSONRegex, BSONArray, BSONBoolean }
import scala.concurrent.duration._

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.Implicits.LilaBSONDocumentZero
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
        case challenges                                  => challenges.drop(maxPerUser).map(_.id).map(remove).sequenceFu.void
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

  def like(c: Challenge) = ~(for {
    challengerId <- c.challengerUserId
    destUserId <- c.destUserId
    if c.active
  } yield coll.find(selectCreated ++ BSONDocument(
    "challenger.id" -> challengerId,
    "destUser.id" -> destUserId)).one[Challenge])

  private[challenge] def countCreatedByDestId(userId: String): Fu[Int] =
    coll.count(Some(selectCreated ++ BSONDocument("destUser.id" -> userId)))

  private[challenge] def realTimeUnseenSince(date: DateTime, max: Int): Fu[List[Challenge]] =
    coll.find(selectCreated ++ selectClock ++ BSONDocument(
      "seenAt" -> BSONDocument("$lt" -> date)
    )).cursor[Challenge]().collect[List](max)

  private[challenge] def expiredIds(max: Int): Fu[List[Challenge.ID]] =
    coll.distinct[Challenge.ID, List](
      "_id",
      BSONDocument("expiresAt" -> BSONDocument("$lt" -> DateTime.now)).some
    )

  def setSeenAgain(id: Challenge.ID) = coll.update(
    selectId(id),
    BSONDocument(
      "$set" -> BSONDocument(
        "status" -> Status.Created.id,
        "seenAt" -> DateTime.now,
        "expiresAt" -> inTwoWeeks))
  ).void

  def setSeen(id: Challenge.ID) = coll.update(
    selectId(id),
    BSONDocument("$set" -> BSONDocument("seenAt" -> DateTime.now))
  ).void

  def offline(challenge: Challenge) = setStatus(challenge, Status.Offline, Some(_ plusHours 3))
  def cancel(challenge: Challenge) = setStatus(challenge, Status.Canceled, Some(_ plusHours 3))
  def decline(challenge: Challenge) = setStatus(challenge, Status.Declined, Some(_ plusHours 3))
  def accept(challenge: Challenge) = setStatus(challenge, Status.Accepted, Some(_ plusHours 3))

  def statusById(id: Challenge.ID) = coll.find(
    selectId(id),
    BSONDocument("status" -> true, "_id" -> false)
  ).one[BSONDocument].map { _.flatMap(_.getAs[Status]("status")) }

  private def setStatus(
    challenge: Challenge,
    status: Status,
    expiresAt: Option[DateTime => DateTime] = None) = coll.update(
    selectCreated ++ selectId(challenge.id),
    BSONDocument("$set" -> BSONDocument(
      "status" -> status.id,
      "expiresAt" -> expiresAt.fold(inTwoWeeks) { _(DateTime.now) }
    ))
  ).void

  private[challenge] def remove(id: Challenge.ID) = coll.remove(selectId(id)).void

  private def selectId(id: Challenge.ID) = BSONDocument("_id" -> id)
  private val selectCreated = BSONDocument("status" -> Status.Created.id)
  private val selectClock = BSONDocument("timeControl.l" -> BSONDocument("$exists" -> true))
}

