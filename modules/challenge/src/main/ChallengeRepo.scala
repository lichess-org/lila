package lila.challenge

import scala.annotation.nowarn

import lila.common.config.Max
import lila.db.dsl.{ *, given }
import lila.user.User

final private class ChallengeRepo(colls: ChallengeColls)(using
    ec: Executor
):

  import BSONHandlers.given
  import Challenge.*

  private val coll = colls.challenge

  private val maxOutgoing = lila.game.Game.maxPlayingRealtime

  def byId(id: Challenge.ID) = coll.find($id(id)).one[Challenge]

  def byIdFor(id: Challenge.ID, dest: lila.user.User) =
    coll.find($id(id) ++ $doc("destUser.id" -> dest.id)).one[Challenge]
  def byIdBy(id: Challenge.ID, orig: lila.user.User) =
    coll.find($id(id) ++ $doc("challenger.id" -> orig.id)).one[Challenge]

  def exists(id: Challenge.ID) = coll.countSel($id(id)).dmap(0 <)

  def insert(c: Challenge): Funit =
    coll.insert.one(c) >> c.challengerUser.?? { challenger =>
      createdByChallengerId()(challenger.id).flatMap {
        case challenges if challenges.sizeIs <= maxOutgoing => funit
        case challenges => challenges.drop(maxOutgoing).map(_.id).map(remove).parallel.void
      }
    }

  def update(c: Challenge): Funit = coll.update.one($id(c.id), c).void

  private def createdList(selector: Bdoc, max: Int): Fu[List[Challenge]] =
    coll.find(selectCreated ++ selector).sort($sort asc "createdAt").cursor[Challenge]().list(max)

  def createdByChallengerId(max: Int = 50)(userId: UserId): Fu[List[Challenge]] =
    createdList($doc("challenger.id" -> userId), max)

  def createdByDestId(max: Int = 50)(userId: UserId): Fu[List[Challenge]] =
    createdList($doc("destUser.id" -> userId), max)

  def createdByPopularDestId(max: Int = 50)(userId: UserId): Fu[List[Challenge]] = for {
    realTime <- createdList($doc("destUser.id" -> userId, "timeControl.l" $exists true), max)
    corres <- (realTime.sizeIs < max) ?? createdList(
      $doc($doc("destUser.id" -> userId), "timeControl.l" $exists false),
      max - realTime.size
    )
  } yield realTime ::: corres

  def setChallenger(c: Challenge, color: Option[chess.Color]) =
    coll.update
      .one(
        $id(c.id),
        $set($doc("challenger" -> c.challenger) ++ color.?? { c =>
          $doc("colorChoice" -> Challenge.ColorChoice(c), "finalColor" -> c)
        })
      )
      .void

  private[challenge] def allWithUserId(userId: UserId): Fu[List[Challenge]] =
    createdByChallengerId()(userId) zip createdByDestId()(userId) dmap { case (x, y) =>
      x ::: y
    }

  private def sameOrigAndDest(c: Challenge) =
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

  private[challenge] def insertIfMissing(c: Challenge) = sameOrigAndDest(c) flatMap {
    case Some(prev) if prev.rematchOf.exists(c.rematchOf.has) => funit
    case Some(prev) if prev.id == c.id                        => funit
    case Some(prev)                                           => cancel(prev) >> insert(c)
    case None                                                 => insert(c)
  }

  private[challenge] def countCreatedByDestId(userId: UserId): Fu[Int] =
    coll.countSel(selectCreated ++ $doc("destUser.id" -> userId))

  private[challenge] def realTimeUnseenSince(date: DateTime, max: Int): Fu[List[Challenge]] =
    coll
      .find(
        $doc(
          "seenAt" $lt date,
          "status" -> Status.Created.id,
          "timeControl.l" $exists true
        )
      )
      .cursor[Challenge]()
      .list(max)

  private[challenge] def expired(max: Int): Fu[List[Challenge]] =
    coll.list[Challenge]("expiresAt" $lt nowDate, max)

  def setSeenAgain(id: Challenge.ID) =
    coll.update
      .one(
        $id(id),
        $doc(
          "$set" -> $doc(
            "status"    -> Status.Created.id,
            "seenAt"    -> nowDate,
            "expiresAt" -> inTwoWeeks
          )
        )
      )
      .void

  def setSeen(id: Challenge.ID) =
    coll.updateField($id(id), "seenAt", nowDate).void

  def offline(challenge: Challenge) = setStatus(challenge, Status.Offline, Some(_ plusHours 3))
  def cancel(challenge: Challenge)  = setStatus(challenge, Status.Canceled, Some(_ plusHours 3))
  def decline(challenge: Challenge, reason: Challenge.DeclineReason) =
    setStatus(challenge, Status.Declined, Some(_ plusHours 3)) >> {
      (reason != Challenge.DeclineReason.default) ??
        coll.updateField($id(challenge.id), "declineReason", reason).void
    }
  private[challenge] def accept(challenge: Challenge) =
    setStatus(challenge, Status.Accepted, Some(_ plusHours 3))

  def statusById(id: Challenge.ID) = coll.primitiveOne[Status]($id(id), "status")

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
            "expiresAt" -> expiresAt.fold(inTwoWeeks) { _(nowDate) }
          )
        )
      )
      .void

  private[challenge] def remove(id: Challenge.ID) = coll.delete.one($id(id)).void

  private val selectCreated = $doc("status" -> Status.Created.id)
