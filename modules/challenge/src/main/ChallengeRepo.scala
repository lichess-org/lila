package lila.challenge

import com.github.ghik.silencer.silent
import org.joda.time.DateTime

import lila.common.config.Max
import lila.db.dsl._

final private class ChallengeRepo(coll: Coll, maxPerUser: Max) {

  import BSONHandlers._
  import Challenge._

  def byId(id: Challenge.ID) = coll.ext.find($id(id)).one[Challenge]

  def byIdFor(id: Challenge.ID, dest: lila.user.User) =
    coll.ext.find($id(id) ++ $doc("destUser.id" -> dest.id)).one[Challenge]

  def exists(id: Challenge.ID) = coll.countSel($id(id)).dmap(0 <)

  def insert(c: Challenge): Funit =
    coll.insert.one(c) >> c.challenger.toOption.?? { challenger =>
      createdByChallengerId(challenger.id).flatMap {
        case challenges if maxPerUser >= challenges.size => funit
        case challenges                                  => challenges.drop(maxPerUser.value).map(_.id).map(remove).sequenceFu.void
      }
    }

  def update(c: Challenge): Funit = coll.update.one($id(c.id), c).void

  def createdByChallengerId(userId: String): Fu[List[Challenge]] =
    coll.ext
      .find(selectCreated ++ $doc("challenger.id" -> userId))
      .sort($doc("createdAt" -> 1))
      .list[Challenge]()

  def createdByDestId(userId: String): Fu[List[Challenge]] =
    coll.ext
      .find(selectCreated ++ $doc("destUser.id" -> userId))
      .sort($doc("createdAt" -> 1))
      .list[Challenge]()

  private[challenge] def allWithUserId(userId: String): Fu[List[Challenge]] =
    createdByChallengerId(userId) |+| createdByDestId(userId)

  @silent def like(c: Challenge) =
    ~(for {
      challengerId <- c.challengerUserId
      destUserId   <- c.destUserId
      if c.active
    } yield coll.one[Challenge](
      selectCreated ++ $doc(
        "challenger.id" -> challengerId,
        "destUser.id"   -> destUserId
      )
    ))

  private[challenge] def countCreatedByDestId(userId: String): Fu[Int] =
    coll.countSel(selectCreated ++ $doc("destUser.id" -> userId))

  private[challenge] def realTimeUnseenSince(date: DateTime, max: Int): Fu[List[Challenge]] =
    coll.ext
      .find(
        selectCreated ++ selectClock ++ $doc(
          "seenAt" -> $doc("$lt" -> date)
        )
      )
      .cursor[Challenge]()
      .gather[List](max)

  private[challenge] def expired(max: Int): Fu[List[Challenge]] =
    coll.ext.find($doc("expiresAt" -> $lt(DateTime.now))).list[Challenge](max)

  def setSeenAgain(id: Challenge.ID) =
    coll.update
      .one(
        $id(id),
        $doc(
          "$set" -> $doc(
            "status"    -> Status.Created.id,
            "seenAt"    -> DateTime.now,
            "expiresAt" -> inTwoWeeks
          )
        )
      )
      .void

  def setSeen(id: Challenge.ID) =
    coll.update
      .one(
        $id(id),
        $doc("$set" -> $doc("seenAt" -> DateTime.now))
      )
      .void

  def offline(challenge: Challenge) = setStatus(challenge, Status.Offline, Some(_ plusHours 3))
  def cancel(challenge: Challenge)  = setStatus(challenge, Status.Canceled, Some(_ plusHours 3))
  def decline(challenge: Challenge) = setStatus(challenge, Status.Declined, Some(_ plusHours 3))
  def accept(challenge: Challenge)  = setStatus(challenge, Status.Accepted, Some(_ plusHours 3))

  def statusById(id: Challenge.ID) =
    coll.ext
      .find(
        $id(id),
        $doc("status" -> true, "_id" -> false)
      )
      .one[Bdoc]
      .map { _.flatMap(_.getAsOpt[Status]("status")) }

  private def setStatus(
      challenge: Challenge,
      status: Status,
      expiresAt: Option[DateTime => DateTime]
  ) =
    coll.update
      .one(
        selectCreated ++ $id(challenge.id),
        $doc(
          "$set" -> $doc(
            "status"    -> status.id,
            "expiresAt" -> expiresAt.fold(inTwoWeeks) { _(DateTime.now) }
          )
        )
      )
      .void

  private[challenge] def remove(id: Challenge.ID) = coll.delete.one($id(id)).void

  private val selectCreated = $doc("status" -> Status.Created.id)
  private val selectClock   = $doc("timeControl.l" $exists true)
}
