package lila.game

import chess.Status
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

object Query:

  import Game.BSONFields as F

  val rated: Bdoc = F.rated.$eq(true)

  def rated(u: UserId): Bdoc = user(u) ++ rated

  def status(s: Status) = F.status.$eq(s.id)

  val created: Bdoc = F.status.$eq(Status.Created.id)

  val started: Bdoc = F.status.$gte(Status.Started.id)

  def started(u: UserId): Bdoc = user(u) ++ started

  val playable: Bdoc = F.status.$lt(Status.Aborted.id)

  val mate: Bdoc = status(Status.Mate)

  def draw(u: UserId): Bdoc =
    user(u) ++ finished ++ F.winnerId.$exists(false) ++ $or(notAi, status(Status.Draw))

  val finished: Bdoc = F.status.$gte(Status.Mate.id)

  val notFinished: Bdoc = F.status.$lte(Status.Started.id)

  def analysed(an: Boolean): Bdoc =
    if an then F.analysed.$eq(true)
    else F.analysed.$ne(true)

  val frozen: Bdoc = F.status.$gte(Status.Mate.id)

  def imported(u: UserId): Bdoc = s"${F.pgnImport}.user".$eq(u)
  def importedSort: Bdoc = $sort.desc(s"${F.pgnImport}.ca")

  val friend: Bdoc = F.source.$eq(lila.core.game.Source.Friend.id)
  val notAi: Bdoc = F.source.$ne(lila.core.game.Source.Ai.id)

  def clock(c: Boolean): Bdoc = F.clock.$exists(c)

  def clockHistory(c: Boolean): Bdoc = F.whiteClockHistory.$exists(c)

  def user[U: UserIdOf](u: U): Bdoc = F.playerUids.$eq(u.id)
  def users(u: Iterable[UserId]): Bdoc = F.playerUids.$in(u)

  val noAnon = $doc(
    "p0.e".$exists(true),
    "p1.e".$exists(true)
  )

  val noAi: Bdoc = $doc(
    "p0.ai".$exists(false),
    "p1.ai".$exists(false)
  )

  val hasAi: Bdoc = $or(
    "p0.ai".$exists(true),
    "p1.ai".$exists(true)
  )

  def nowPlaying[U: UserIdOf](u: U) = $doc(F.playingUids -> u.id)

  def recentlyPlaying(u: UserId) =
    nowPlaying(u) ++ $doc(F.movedAt.$gt(nowInstant.minusMinutes(5)))

  def nowPlayingVs(u1: UserId, u2: UserId) = $doc(F.playingUids.$all(List(u1, u2)))

  def nowPlayingVs(userIds: Iterable[UserId]) =
    $doc(
      F.playingUids.$in(userIds), // as to use the index
      s"${F.playingUids}.0".$in(userIds),
      s"${F.playingUids}.1".$in(userIds)
    )

  // use the us index
  def win(u: UserId) = user(u) ++ $doc(F.winnerId -> u)

  def loss(u: UserId) =
    user(u) ++ $doc(
      F.status.$in(Status.finishedWithWinner.map(_.id)),
      F.winnerId -> $doc(
        "$exists" -> true,
        "$ne" -> u
      )
    )

  def opponents(u1: User, u2: User) =
    $doc(F.playerUids.$all(List(u1, u2).sortBy(_.count.game).map(_.id)))

  def opponents(userIds: Iterable[UserId]) =
    $doc(
      F.playerUids.$in(userIds), // as to use the index
      s"${F.playerUids}.0".$in(userIds),
      s"${F.playerUids}.1".$in(userIds)
    )

  val noProvisional: Bdoc = $doc(
    "p0.p".$exists(false),
    "p1.p".$exists(false)
  )

  def bothRatingsGreaterThan(v: Int) = $doc("p0.e".$gt(v), "p1.e".$gt(v))

  def turnsGt(nb: Int) = F.turns.$gt(nb)
  def turns(range: PairOf[Int]) = F.turns.$inRange(range)

  def checkable = F.checkAt.$lt(nowInstant)

  def checkableOld = F.checkAt.$lt(nowInstant.minusHours(1))

  def variant(v: chess.variant.Variant) =
    $doc(F.variant -> (if v.standard then $exists(false) else $int(v.id)))

  lazy val variantStandard = variant(chess.variant.Standard)

  lazy val notHordeOrSincePawnsAreWhite: Bdoc = $or(
    F.variant.$ne(chess.variant.Horde.id),
    sinceHordePawnsAreWhite
  )

  lazy val sinceHordePawnsAreWhite: Bdoc =
    createdSince(Game.hordeWhitePawnsSince)

  val notFromPosition: Bdoc =
    F.variant.$ne(chess.variant.FromPosition.id)

  def createdSince(d: Instant): Bdoc =
    F.createdAt.$gte(d)

  def createdBetween(since: Option[Instant], until: Option[Instant]): Bdoc =
    dateBetween(F.createdAt, since, until)

  val notSimul = F.simulId.$exists(false)

  val sortCreated: Bdoc = $sort.desc(F.createdAt)
  val sortChronological: Bdoc = $sort.asc(F.createdAt)
  val sortAntiChronological: Bdoc = $sort.desc(F.createdAt)
  val sortMovedAtNoIndex: Bdoc = $sort.desc(F.movedAt)
