package lila.challenge

import org.joda.time.DateTime

import lila.db.dsl._

private final class ChallengeRepo(coll: Coll, maxPerUser: Int) {

  import BSONHandlers._
  import Challenge._

  def byId(id: Challenge.ID): Fu[Option[Challenge]] =
    coll.find($id(id)).one[Challenge]

  def byIdFor(id: Challenge.ID, dest: lila.user.User): Fu[Option[Challenge]] =
    coll.find($id(id) ++ $doc("destUser.id" -> dest.id)).one[Challenge]

  def exists(id: Challenge.ID): Fu[Boolean] = coll.countSel($id(id)).map(0<)

  def insert(c: Challenge): Funit =
    coll.insert.one(c) >> c.challenger.right.toOption.?? { challenger =>
      createdByChallengerId(challenger.id).flatMap {
        case challenges if challenges.size <= maxPerUser => funit

        case challenges =>
          challenges.drop(maxPerUser).map(_.id).map(remove).sequenceFu.void
      }
    }

  def update(c: Challenge): Funit = coll.update.one($id(c.id), c).void

  def createdByChallengerId(userId: String): Fu[List[Challenge]] =
    coll.find(selectCreated ++ $doc("challenger.id" -> userId))
      .sort($doc("createdAt" -> 1)).cursor[Challenge]().list

  def createdByDestId(userId: String): Fu[List[Challenge]] =
    coll.find(selectCreated ++ $doc("destUser.id" -> userId))
      .sort($doc("createdAt" -> 1)).cursor[Challenge]().list

  private[challenge] def allWithUserId(userId: String): Fu[List[Challenge]] =
    createdByChallengerId(userId) |+| createdByDestId(userId)

  def like(c: Challenge) = ~(for {
    challengerId <- c.challengerUserId
    destUserId <- c.destUserId
    if c.active
  } yield coll.find(selectCreated ++ $doc(
    "challenger.id" -> challengerId,
    "destUser.id" -> destUserId
  )).one[Challenge])

  private[challenge] def countCreatedByDestId(userId: String): Fu[Int] =
    coll.countSel(selectCreated ++ $doc("destUser.id" -> userId))

  private[challenge] def realTimeUnseenSince(date: DateTime, max: Int): Fu[List[Challenge]] =
    coll.find(selectCreated ++ selectClock ++ $doc(
      "seenAt" -> $doc("$lt" -> date)
    )).cursor[Challenge]().list(max)

  private[challenge] def expired(max: Int): Fu[List[Challenge]] =
    coll.find($doc("expiresAt" -> $lt(DateTime.now)))
      .cursor[Challenge]().list(max)

  def setSeenAgain(id: Challenge.ID): Funit = coll.update.one($id(id), $doc(
    "$set" -> $doc(
      "status" -> Status.Created.id,
      "seenAt" -> DateTime.now,
      "expiresAt" -> inTwoWeeks
    )
  )).void

  def setSeen(id: Challenge.ID): Funit =
    coll.update.one($id(id), $set($doc("seenAt" -> DateTime.now))).void

  def offline(challenge: Challenge): Funit =
    setStatus(challenge, Status.Offline, Some(_ plusHours 3))

  def cancel(challenge: Challenge): Funit =
    setStatus(challenge, Status.Canceled, Some(_ plusHours 3))

  def decline(challenge: Challenge): Funit =
    setStatus(challenge, Status.Declined, Some(_ plusHours 3))

  def accept(challenge: Challenge): Funit =
    setStatus(challenge, Status.Accepted, Some(_ plusHours 3))

  def statusById(id: Challenge.ID): Fu[Option[Status]] = coll.find(
    selector = $id(id),
    projection = Some($doc("status" -> true, "_id" -> false))
  ).one[Bdoc].map { _.flatMap(_.getAs[Status]("status")) }

  private def setStatus(
    challenge: Challenge,
    status: Status,
    expiresAt: Option[DateTime => DateTime]
  ): Funit = coll.update.one(
    q = selectCreated ++ $id(challenge.id),
    u = $set($doc(
      "status" -> status.id,
      "expiresAt" -> expiresAt.fold(inTwoWeeks) { _(DateTime.now) }
    ))
  ).void

  private[challenge] def remove(id: Challenge.ID): Funit =
    coll.delete.one($id(id)).void

  private val selectCreated = $doc("status" -> Status.Created.id)
  private val selectClock = $doc("timeControl.l" $exists true)
}

