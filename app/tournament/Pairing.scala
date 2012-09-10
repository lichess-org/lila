package lila
package tournament

import game.IdGenerator

case class Pairing(
    gameId: String,
    status: chess.Status,
    user1: String,
    user2: String) {

  def encode: RawPairing = RawPairing(gameId, status.id, users)

  def users = List(user1, user2)
  def usersPair = user1 -> user2
  def contains(user: String) = user1 == user || user2 == user

  def finished = status >= chess.Status.Mate
  def playing = !finished

  def withStatus(s: chess.Status) = copy(status = s)
}

case class RawPairing(g: String, s: Int, u: List[String]) {

  def decode: Option[Pairing] = for {
    status ← chess.Status(s)
    user1 ← u.lift(0)
    user2 ← u.lift(1)
  } yield Pairing(g, status, user1, user2)
}

object Pairing {

  def apply(user1: String, user2: String): Pairing = new Pairing(
    gameId = IdGenerator.game,
    status = chess.Status.Created,
    user1 = user1,
    user2 = user2)

  def createNewPairings(users: List[String], pairings: Pairings): Pairings = {
    val idleUsers: List[String] = pairings.filter(_.playing).foldLeft(users) {
      (idles, pairing) ⇒ idles filterNot pairing.contains
    }.toList
    naivePairing(idleUsers).toList
  }

  private def naivePairing(users: List[String]) = users grouped 2 collect {
    case List(u1, u2) ⇒ Pairing(u1, u2)
  }

}
