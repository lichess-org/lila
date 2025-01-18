package lila.challenge

import lila.db.dsl.{ *, given }

final private class ChallengeRepo(colls: ChallengeColls)(using
    ec: Executor
):

  import BSONHandlers.given
  import Challenge.*

  private val coll = colls.challenge

  import lila.core.game.maxPlayingRealtime

  def byId(id: ChallengeId) = coll.find($id(id)).one[Challenge]

  def byIdFor(id: ChallengeId, dest: User) =
    coll.find($id(id) ++ $doc("destUser.id" -> dest.id)).one[Challenge]

  def exists(id: ChallengeId) = coll.countSel($id(id)).dmap(0 <)

  def insert(c: Challenge): Funit =
    coll.insert.one(c) >> c.challengerUser.so: challenger =>
      createdByChallengerId()(challenger.id).flatMap:
        case challenges if challenges.sizeIs <= maxPlayingRealtime.value => funit
        case challenges => challenges.drop(maxPlayingRealtime.value).map(_.id).sequentiallyVoid(remove)

  def update(c: Challenge): Funit = coll.update.one($id(c.id), c).void

  private def createdList(selector: Bdoc, max: Int): Fu[List[Challenge]] =
    coll.find(selectCreated ++ selector).sort($sort.asc("createdAt")).cursor[Challenge]().list(max)

  def createdByChallengerId(max: Int = 50)(userId: UserId): Fu[List[Challenge]] =
    createdList($doc("challenger.id" -> userId), max)

  def createdByDestId(max: Int = 50)(userId: UserId): Fu[List[Challenge]] =
    createdList($doc("destUser.id" -> userId), max)

  def createdByPopularDestId(max: Int = 50)(userId: UserId): Fu[List[Challenge]] = for
    realTime <- createdList($doc("destUser.id" -> userId, "timeControl.l".$exists(true)), max)
    corres <- (realTime.sizeIs < max).so(
      createdList(
        $doc($doc("destUser.id" -> userId), "timeControl.l".$exists(false)),
        max - realTime.size
      )
    )
  yield realTime ::: corres

  def setChallenger(c: Challenge, color: Option[Color]) =
    coll.update
      .one(
        $id(c.id),
        $set($doc("challenger" -> c.challenger) ++ color.so { c =>
          $doc("colorChoice" -> Challenge.ColorChoice(c), "finalColor" -> c)
        })
      )
      .void

  private[challenge] def allWithUserId(userId: UserId): Fu[List[Challenge]] =
    (createdByChallengerId()(userId), createdByDestId()(userId)).mapN(_ ::: _)

  private def sameOrigAndDest(c: Challenge): Fu[Option[Challenge]] =
    ~(for
      challengerSelect <- c.challenger match
        case Challenger.Registered(uid, _) => some("challenger.id" -> uid.value)
        case Challenger.Anonymous(sid)     => some("challenger.s" -> sid)
        case _                             => none
      destUserId <- c.destUserId
      if c.active
    yield coll.one[Challenge](
      selectCreated ++ $doc(
        challengerSelect,
        "destUser.id" -> destUserId
      )
    ))

  private[challenge] def insertIfMissing(c: Challenge) = sameOrigAndDest(c).flatMap:
    case Some(prev) if prev.rematchOf.exists(c.rematchOf.has) => funit
    case Some(prev) if prev.id == c.id                        => funit
    case Some(prev)                                           => cancel(prev) >> insert(c)
    case None                                                 => insert(c)

  private[challenge] def countCreatedByDestId(userId: UserId): Fu[Int] =
    coll.countSel(selectCreated ++ $doc("destUser.id" -> userId))

  private[challenge] def realTimeUnseenSince(date: Instant, max: Int): Fu[List[Challenge]] =
    coll
      .find(
        $doc(
          "seenAt".$lt(date),
          "status" -> Status.Created.id,
          "timeControl.l".$exists(true)
        )
      )
      .cursor[Challenge]()
      .list(max)

  private[challenge] def expired(max: Int): Fu[List[Challenge]] =
    coll.list[Challenge]("expiresAt".$lt(nowInstant), max)

  def setSeenAgain(id: ChallengeId) =
    coll.update
      .one(
        $id(id),
        $doc(
          "$set" -> $doc(
            "status"    -> Status.Created.id,
            "seenAt"    -> nowInstant,
            "expiresAt" -> inTwoWeeks
          )
        )
      )
      .void

  def setSeen(id: ChallengeId) =
    coll.updateField($id(id), "seenAt", nowInstant).void

  def offline(challenge: Challenge) = setStatus(challenge, Status.Offline, Some(_.plusHours(3)))
  def cancel(challenge: Challenge)  = setStatus(challenge, Status.Canceled, Some(_.plusHours(3)))
  def decline(challenge: Challenge, reason: Challenge.DeclineReason) =
    setStatus(challenge, Status.Declined, Some(_.plusHours(3))) >> {
      (reason != Challenge.DeclineReason.default)
        .so(coll.updateField($id(challenge.id), "declineReason", reason).void)
    }
  private[challenge] def accept(challenge: Challenge) =
    setStatus(challenge, Status.Accepted, Some(_.plusHours(3)))

  def statusById(id: ChallengeId) = coll.primitiveOne[Status]($id(id), "status")

  private def setStatus(challenge: Challenge, status: Status, expiresAt: Option[Instant => Instant]) =
    coll.update
      .one(
        selectCreatedOrOffline ++ $id(challenge.id),
        $doc(
          "$set" -> $doc(
            "status"    -> status.id,
            "expiresAt" -> expiresAt.fold(inTwoWeeks) { _(nowInstant) }
          )
        )
      )
      .void

  private[challenge] def remove(id: ChallengeId) = coll.delete.one($id(id)).void

  private val selectCreated          = $doc("status" -> Status.Created)
  private val selectCreatedOrOffline = $doc("status".$in(List(Status.Created, Status.Offline)))
