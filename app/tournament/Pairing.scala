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
  def notContains(user: String) = !contains(user)

  def finished = status >= chess.Status.Mate
  def playing = !finished

  def opponentOf(user: String): Option[String] =
    if (user == user1) user2.some else if (user == user2) user1.some else none

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

  def apply(users: (String, String)): Pairing = apply(users._1, users._2)
  def apply(user1: String, user2: String): Pairing = new Pairing(
    gameId = IdGenerator.game,
    status = chess.Status.Created,
    user1 = user1,
    user2 = user2)

  def createNewPairings(users: List[String], pairings: Pairings): Pairings = {
    val idles: List[String] = users.toSet diff {
      (pairings filter (_.playing) flatMap (_.users)).toSet
    } toList

    def lastOpponent(user: String): Option[String] =
      pairings find (_ contains user) flatMap (_ opponentOf user)

    def justPlayedTogether(u1: String, u2: String): Boolean =
      lastOpponent(u1) == u2.some && lastOpponent(u2) == u1.some

    def timeSincePlay(u: String): Int =
      pairings.takeWhile(_ notContains u).size

    // lower is better
    def score(u1: String, u2: String): Int = justPlayedTogether(u1, u2).fold(
      100,
      50 - timeSincePlay(u1) - timeSincePlay(u2)
    )

    def allPairs(us: List[String]): List[(String, String)] = 
      for(x ← us; y ← us.dropWhile(x!=).tail) yield x -> y

    val pairs = idles match {
      case x if x.size < 2                            ⇒ Nil
      case List(u1, u2) if justPlayedTogether(u1, u2) ⇒ Nil
      case List(u1, u2)                               ⇒ List(u1 -> u2)
      case us ⇒ {
        allPairs(us) map { case p @ (a, b) ⇒ score(a, b) -> p } sortBy (_._1)
      }.headOption.map(_._2).getOrElse(Nil)
    }

    pairs map Pairing.apply
  }

  private def naivePairing(users: List[String]) = users grouped 2 collect {
    case List(u1, u2) ⇒ Pairing(u1, u2)
  } toList

}
