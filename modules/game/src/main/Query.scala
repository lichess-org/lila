package lila.game

import chess.{ Color, Status }
import org.joda.time.DateTime
import reactivemongo.bson._

import lila.db.dsl._
import lila.user.User

object Query {

  import Game.{ BSONFields => F }

  val rated: BSONDocument = $doc(F.rated -> true)

  def rated(u: String): BSONDocument = user(u) ++ rated

  def status(s: Status) = $doc(F.status -> s.id)

  val created = $doc(F.status -> Status.Created.id)

  val started: BSONDocument = $doc(F.status -> $gte(Status.Started.id))

  def started(u: String): BSONDocument = user(u) ++ started

  val playable = $doc(F.status -> $lt(Status.Aborted.id))

  val mate = status(Status.Mate)

  val draw: BSONDocument = $doc(F.status -> $in(Seq(Status.Draw.id, Status.Stalemate.id)))

  def draw(u: String): BSONDocument = user(u) ++ draw

  val finished = $doc(F.status -> $gte(Status.Mate.id))

  val notFinished = $doc(F.status -> $lte(Status.Started.id))

  def analysed(an: Boolean) = $doc(F.analysed -> an)

  def turnsMoreThan(length: Int) = $doc(F.turns -> $gte(length))

  val frozen = $doc(F.status -> $gte(Status.Mate.id))

  def imported(u: String) = $doc(s"${F.pgnImport}.user" -> u)

  val friend = $doc(s"${F.source}" -> Source.Friend.id)

  def clock(c: Boolean): BSONDocument = F.clock $exists c

  def user(u: String): BSONDocument = $doc(F.playerUids -> u)
  def user(u: User): BSONDocument = $doc(F.playerUids -> u.id)
  def users(u: Seq[String]) = $doc(F.playerUids -> $in(u))

  val noAi = $doc(
    "p0.ai" -> $exists(false),
    "p1.ai" -> $exists(false))

  def nowPlaying(u: String) = $doc(F.playingUids -> u)

  def recentlyPlaying(u: String) =
    nowPlaying(u) ++ $doc(
      F.updatedAt -> $gt($date(DateTime.now minusMinutes 5))
    )

  // use the us index
  def win(u: String) = user(u) ++ $doc(F.winnerId -> u)

  def loss(u: String) = user(u) ++
    $doc(F.status -> $in(Status.finishedWithWinner map (_.id))) ++
    $doc(F.winnerId -> ($ne(u) ++ $exists(true)))

  def opponents(u1: User, u2: User) =
    $doc(F.playerUids -> $all(List(u1, u2).sortBy(_.count.game).map(_.id)))

  val noProvisional = $doc("p0.p" -> $exists(false), "p1.p" -> $exists(false))

  def bothRatingsGreaterThan(v: Int) = $doc("p0.e" -> $gt(v), "p1.e" -> $gt(v))

  def turnsGt(nb: Int) = $doc(F.turns -> $gt(nb))

  def checkable = $doc(F.checkAt -> $lt($date(DateTime.now)))

  def variant(v: chess.variant.Variant) =
    $doc(F.variant -> v.standard.fold($exists(false), v.id))

  lazy val notHordeOrSincePawnsAreWhite = $or(Seq(
    $doc(F.variant -> $ne(chess.variant.Horde.id)),
    sinceHordePawnsAreWhite
  ))

  lazy val sinceHordePawnsAreWhite =
    $doc(F.createdAt -> $gt($date(hordeWhitePawnsSince)))

  val hordeWhitePawnsSince = new DateTime(2015, 4, 11, 10, 0)

  def notFromPosition =
    $doc(F.variant -> $ne(chess.variant.FromPosition.id))

  def createdSince(d: DateTime) =
    $doc(F.createdAt -> $gt($date(d)))

  val sortCreated = $sort desc F.createdAt
  val sortChronological = $sort asc F.createdAt
  val sortAntiChronological = $sort desc F.createdAt
  val sortUpdatedNoIndex = $sort desc F.updatedAt
}
