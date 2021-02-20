package lila.game

import chess.Status
import org.joda.time.DateTime
import reactivemongo.api.bson._

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._
import lila.user.User

object Query {

  import Game.{ BSONFields => F }

  val rated: Bdoc = F.rated $eq true

  def rated(u: String): Bdoc = user(u) ++ rated

  def status(s: Status) = F.status $eq s.id

  val created: Bdoc = F.status $eq Status.Created.id

  val started: Bdoc = F.status $gte Status.Started.id

  def started(u: String): Bdoc = user(u) ++ started

  val playable: Bdoc = F.status $lt Status.Aborted.id

  val mate: Bdoc = status(Status.Mate)

  def draw(u: String): Bdoc = user(u) ++ finished ++ F.winnerId.$exists(false)

  val finished: Bdoc = F.status $gte Status.Mate.id

  val notFinished: Bdoc = F.status $lte Status.Started.id

  def analysed(an: Boolean): Bdoc =
    if (an) F.analysed $eq true
    else F.analysed $ne true

  val frozen: Bdoc = F.status $gte Status.Mate.id

  def imported(u: String): Bdoc = s"${F.pgnImport}.user" $eq u

  val friend: Bdoc = s"${F.source}" $eq Source.Friend.id

  def clock(c: Boolean): Bdoc = F.clock $exists c

  def clockHistory(c: Boolean): Bdoc = F.whiteClockHistory $exists c

  def user(u: String): Bdoc = F.playerUids $eq u
  def user(u: User): Bdoc   = F.playerUids $eq u.id

  val noAi: Bdoc = $doc(
    "p0.ai" $exists false,
    "p1.ai" $exists false
  )

  def nowPlaying(u: String) = $doc(F.playingUids -> u)

  def recentlyPlaying(u: String) =
    nowPlaying(u) ++ $doc(F.movedAt $gt DateTime.now.minusMinutes(5))

  def nowPlayingVs(u1: String, u2: String) = $doc(F.playingUids $all List(u1, u2))

  def nowPlayingVs(userIds: Iterable[String]) =
    $doc(
      F.playingUids $in userIds, // as to use the index
      s"${F.playingUids}.0" $in userIds,
      s"${F.playingUids}.1" $in userIds
    )

  // use the us index
  def win(u: String) = user(u) ++ $doc(F.winnerId -> u)

  def loss(u: String) =
    user(u) ++ $doc(
      F.status $in Status.finishedWithWinner.map(_.id),
      F.winnerId -> $doc(
        "$exists" -> true,
        "$ne"     -> u
      )
    )

  def opponents(u1: User, u2: User) =
    $doc(F.playerUids $all List(u1, u2).sortBy(_.count.game).map(_.id))

  def opponents(userIds: Iterable[String]) =
    $doc(
      F.playerUids $in userIds, // as to use the index
      s"${F.playerUids}.0" $in userIds,
      s"${F.playerUids}.1" $in userIds
    )

  val noProvisional: Bdoc = $doc(
    "p0.p" $exists false,
    "p1.p" $exists false
  )

  def bothRatingsGreaterThan(v: Int) = $doc("p0.e" $gt v, "p1.e" $gt v)

  def turnsGt(nb: Int) = F.turns $gt nb

  def checkable = F.checkAt $lt DateTime.now

  def checkableOld = F.checkAt $lt DateTime.now.minusHours(1)

  def variant(v: chess.variant.Variant) =
    $doc(F.variant -> (if (v.standard) $exists(false) else $int(v.id)))

  lazy val variantStandard = variant(chess.variant.Standard)

  lazy val notHordeOrSincePawnsAreWhite: Bdoc = $or(
    F.variant $ne chess.variant.Horde.id,
    sinceHordePawnsAreWhite
  )

  lazy val sinceHordePawnsAreWhite: Bdoc =
    createdSince(Game.hordeWhitePawnsSince)

  val notFromPosition: Bdoc =
    F.variant $ne chess.variant.FromPosition.id

  def createdSince(d: DateTime): Bdoc =
    F.createdAt $gt d

  def createdBetween(since: Option[DateTime], until: Option[DateTime]): Bdoc =
    (since, until) match {
      case (Some(since), None)        => createdSince(since)
      case (None, Some(until))        => F.createdAt $lt until
      case (Some(since), Some(until)) => F.createdAt $gt since $lt until
      case _                          => $empty
    }

  val sortCreated: Bdoc           = $sort desc F.createdAt
  val sortChronological: Bdoc     = $sort asc F.createdAt
  val sortAntiChronological: Bdoc = $sort desc F.createdAt
  val sortMovedAtNoIndex: Bdoc    = $sort desc F.movedAt
}
