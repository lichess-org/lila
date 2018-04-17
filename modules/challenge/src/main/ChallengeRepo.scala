package lila.challenge

import org.joda.time.DateTime

import lila.db.dsl._

private final class ChallengeRepo(coll: Coll, maxPerUser: Int) {

  import BSONHandlers._
  import Challenge._

  def byId(id: Challenge.ID) = coll.find($id(id)).uno[Challenge]

  def byIdFor(id: Challenge.ID, dest: lila.user.User) =
    coll.find($id(id) ++ $doc("destUser.id" -> dest.id)).uno[Challenge]

  def exists(id: Challenge.ID) = coll.count($id(id).some).map(0<)

  def insert(c: Challenge): Funit =
    coll.insert(c) >> c.challenger.right.toOption.?? { challenger =>
      createdByChallengerId(challenger.id).flatMap {
        case challenges if challenges.size <= maxPerUser => funit
        case challenges => challenges.drop(maxPerUser).map(_.id).map(remove).sequenceFu.void
      }
    }

  def update(c: Challenge): Funit = coll.update($id(c.id), c).void

  def createdByChallengerId(userId: String): Fu[List[Challenge]] =
    coll.find(selectCreated ++ $doc("challenger.id" -> userId))
      .sort($doc("createdAt" -> 1))
      .list[Challenge]()

  def createdByDestId(userId: String): Fu[List[Challenge]] =
    coll.find(selectCreated ++ $doc("destUser.id" -> userId))
      .sort($doc("createdAt" -> 1))
      .list[Challenge]()

  private[challenge] def allWithUserId(userId: String): Fu[List[Challenge]] =
    createdByChallengerId(userId) |+| createdByDestId(userId)

  def like(c: Challenge) = ~(for {
    challengerId <- c.challengerUserId
    destUserId <- c.destUserId
    if c.active
  } yield coll.find(selectCreated ++ $doc(
    "challenger.id" -> challengerId,
    "destUser.id" -> destUserId
  )).uno[Challenge])

  private[challenge] def countCreatedByDestId(userId: String): Fu[Int] =
    coll.count(Some(selectCreated ++ $doc("destUser.id" -> userId)))

  private[challenge] def realTimeUnseenSince(date: DateTime, max: Int): Fu[List[Challenge]] =
    coll.find(selectCreated ++ selectClock ++ $doc(
      "seenAt" -> $doc("$lt" -> date)
    )).cursor[Challenge]().gather[List](max)

  private[challenge] def expired(max: Int): Fu[List[Challenge]] =
    coll.find($doc("expiresAt" -> $lt(DateTime.now))).list[Challenge](max)

  def setSeenAgain(id: Challenge.ID) = coll.update(
    $id(id),
    $doc(
      "$set" -> $doc(
        "status" -> Status.Created.id,
        "seenAt" -> DateTime.now,
        "expiresAt" -> inTwoWeeks
      )
    )
  ).void

  def setSeen(id: Challenge.ID) = coll.update(
    $id(id),
    $doc("$set" -> $doc("seenAt" -> DateTime.now))
  ).void

  def offline(challenge: Challenge) = setStatus(challenge, Status.Offline, Some(_ plusHours 3))
  def cancel(challenge: Challenge) = setStatus(challenge, Status.Canceled, Some(_ plusHours 3))
  def decline(challenge: Challenge) = setStatus(challenge, Status.Declined, Some(_ plusHours 3))
  def accept(challenge: Challenge) = setStatus(challenge, Status.Accepted, Some(_ plusHours 3))

  def statusById(id: Challenge.ID) = coll.find(
    $id(id),
    $doc("status" -> true, "_id" -> false)
  ).uno[Bdoc].map { _.flatMap(_.getAs[Status]("status")) }

  private def setStatus(
    challenge: Challenge,
    status: Status,
    expiresAt: Option[DateTime => DateTime]
  ) = coll.update(
    selectCreated ++ $id(challenge.id),
    $doc("$set" -> $doc(
      "status" -> status.id,
      "expiresAt" -> expiresAt.fold(inTwoWeeks) { _(DateTime.now) }
    ))
  ).void

  private[challenge] def remove(id: Challenge.ID) = coll.remove($id(id)).void

  private val selectCreated = $doc("status" -> Status.Created.id)
  private val selectClock = $doc("timeControl.l" $exists true)
}

