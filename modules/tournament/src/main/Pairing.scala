package lila.tournament

import chess.Color
import lila.game.{ PovRef, IdGenerator }

import org.joda.time.DateTime

case class Pairing(
    gameId: String,
    status: chess.Status,
    user1: String,
    user2: String,
    winner: Option[String],
    turns: Option[Int],
    pairedAt: Option[DateTime]) {

  def encode: RawPairing = RawPairing(gameId, status.id, users, winner, turns, pairedAt)

  def users = List(user1, user2)
  def usersPair = user1 -> user2
  def contains(user: String) = user1 == user || user2 == user
  def notContains(user: String) = !contains(user)

  def finished = status >= chess.Status.Mate
  def playing = !finished

  def lostBy(user: String) = winner.??(user !=)
  def quickLoss = finished && turns.??(20 >)
  def quickDraw = draw && turns.??(20 >)

  def opponentOf(user: String): Option[String] =
    if (user == user1) user2.some else if (user == user2) user1.some else none

  def wonBy(user: String): Boolean = winner.??(user ==)
  def draw: Boolean = finished && winner.isEmpty

  def colorOf(userId: String): Option[Color] =
    if (userId == user1) Color.White.some
    else if (userId == user2) Color.Black.some
    else none

  def povRef(userId: String): Option[PovRef] =
    colorOf(userId) map { PovRef(gameId, _) }

  def withStatus(s: chess.Status) = copy(status = s)

  def finish(s: chess.Status, w: Option[String], t: Int) = copy(
    status = s,
    winner = w,
    turns = t.some
  )
}

private[tournament] object Pairing {
  type P = (String, String)

  def apply(us: P): Pairing = apply(us._1, us._2)
  def apply(u1: String, u2: String): Pairing = apply(u1, u2, None)
  def apply(u1: String, u2: String, pa: DateTime): Pairing = apply(u1, u2, Some(pa))

  def apply(u1: String, u2: String, pa: Option[DateTime]): Pairing = new Pairing(
    gameId = IdGenerator.game,
    status = chess.Status.Created,
    user1 = u1,
    user2 = u2,
    winner = none,
    turns = none,
    pairedAt = pa)
}

private[tournament] case class RawPairing(g: String, s: Int, u: List[String], w: Option[String], t: Option[Int], p: Option[DateTime]) {

  def decode: Option[Pairing] = for {
    status ← chess.Status(s)
    user1 ← u.lift(0)
    user2 ← u.lift(1)
  } yield Pairing(g, status, user1, user2, w, t, p)
}

private[tournament] object RawPairing {

  import lila.db.JsTube
  import JsTube.Helpers._
  import play.api.libs.json._

  private def defaults = Json.obj(
    "w" -> none[String],
    "t" -> none[Int],
    "p" -> none[DateTime])

  private[tournament] val tube = JsTube(
    (__.json update merge(defaults)) andThen Json.reads[RawPairing],
    Json.writes[RawPairing]
  )
}
