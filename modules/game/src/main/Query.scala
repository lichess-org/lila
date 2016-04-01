package lila.game

import chess.{ Color, Status }
import org.joda.time.DateTime
import reactivemongo.bson._

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

  val draw: Bdoc = F.status $in Seq(Status.Draw.id, Status.Stalemate.id)

  def draw(u: String): Bdoc = user(u) ++ draw

  val finished: Bdoc = F.status $gte Status.Mate.id

  val notFinished: Bdoc = F.status $lte Status.Started.id

  def analysed(an: Boolean): Bdoc = F.analysed $eq an

  def turnsMoreThan(length: Int): Bdoc = F.turns $eq $gte(length)

  val frozen: Bdoc = F.status $gte Status.Mate.id

  def imported(u: String): Bdoc = s"${F.pgnImport}.user" $eq u

  val friend: Bdoc = s"${F.source}" $eq Source.Friend.id

  def clock(c: Boolean): Bdoc = F.clock $exists c

  def user(u: String): Bdoc = F.playerUids $eq u
  def user(u: User): Bdoc = F.playerUids $eq u.id
  def users(u: Seq[String]) = F.playerUids $eq $in(u)

  val noAi: Bdoc = $doc(
    "p0.ai" $exists false,
    "p1.ai" $exists false)

  def nowPlaying(u: String) = $doc(F.playingUids -> u)

  def recentlyPlaying(u: String) =
    nowPlaying(u) ++ $doc(F.updatedAt $gt DateTime.now.minusMinutes(5))

  // use the us index
  def win(u: String) = user(u) ++ $doc(F.winnerId -> u)

  def loss(u: String) = user(u) ++ $doc(
    F.status $in Status.finishedWithWinner.map(_.id),
    F.winnerId -> $exists(true).++($ne(u))
  )

  def opponents(u1: User, u2: User) =
    $doc(F.playerUids $all List(u1, u2).sortBy(_.count.game).map(_.id))

  val noProvisional: Bdoc = $doc(
    "p0.p" $exists false,
    "p1.p" $exists false)

  def bothRatingsGreaterThan(v: Int) = $doc("p0.e" $gt v, "p1.e" $gt v)

  def turnsGt(nb: Int) = F.turns $gt nb

  def checkable = F.checkAt $lt DateTime.now

  def variant(v: chess.variant.Variant) =
    $doc(F.variant -> v.standard.fold[BSONValue]($exists(false), $int(v.id)))

  lazy val notHordeOrSincePawnsAreWhite: Bdoc = $or(
    F.variant $ne chess.variant.Horde.id,
    sinceHordePawnsAreWhite
  )

  lazy val sinceHordePawnsAreWhite: Bdoc =
    F.createdAt $gt hordeWhitePawnsSince

  val hordeWhitePawnsSince = new DateTime(2015, 4, 11, 10, 0)

  val notFromPosition: Bdoc =
    F.variant $ne chess.variant.FromPosition.id

  def createdSince(d: DateTime): Bdoc =
    F.createdAt $gt d

  val sortCreated: Bdoc = $sort desc F.createdAt
  val sortChronological: Bdoc = $sort asc F.createdAt
  val sortAntiChronological: Bdoc = $sort desc F.createdAt
  val sortUpdatedNoIndex: Bdoc = $sort desc F.updatedAt
}
