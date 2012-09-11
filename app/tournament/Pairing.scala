package lila
package tournament

import game.IdGenerator

import scala.util.Random

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

  type P = (String, String)

  def apply(users: P): Pairing = apply(users._1, users._2)
  def apply(user1: String, user2: String): Pairing = new Pairing(
    gameId = IdGenerator.game,
    status = chess.Status.Created,
    user1 = user1,
    user2 = user2)

  def createNewPairings(users: List[String], pairings: Pairings): Pairings = {

    val idles: List[String] = Random shuffle {
      users.toSet diff { (pairings filter (_.playing) flatMap (_.users)).toSet } toList
    }

    pairings.isEmpty.fold(
      naivePairings(idles), 
      (idles.size > 12).fold(
        naivePairings(idles),
        smartPairings(idles, pairings)
      )
    )
  }

  private def naivePairings(users: List[String]) = 
    Random shuffle users grouped 2 collect {
      case List(u1, u2) ⇒ Pairing(u1, u2)
    } toList

  private def smartPairings(users: List[String], pairings: Pairings): Pairings = {

    def lastOpponent(user: String): Option[String] =
      pairings find (_ contains user) flatMap (_ opponentOf user)

    def justPlayedTogether(u1: String, u2: String): Boolean =
      lastOpponent(u1) == u2.some && lastOpponent(u2) == u1.some

    def timeSincePlay(u: String): Int =
      pairings.takeWhile(_ notContains u).size

    // lower is better
    def score(pair: P): Int = pair match {
      case (a, b) ⇒ justPlayedTogether(a, b).fold(
        100,
        - timeSincePlay(a) - timeSincePlay(b))
    }

    (users match {
      case x if x.size < 2 ⇒ Nil
      case List(u1, u2) if (justPlayedTogether(u1, u2) && users.size > 2) ⇒ Nil
      case List(u1, u2) ⇒ List(u1 -> u2)
      case us ⇒ allPairCombinations(us)
        .map(c ⇒ c -> c.map(score).sum)
        .sortBy(_._2)
        .headOption
        .map(_._1) | Nil
    }) map Pairing.apply
  }

  def allPairCombinations(list: List[String]): List[List[(String, String)]] = list match {
    case a :: rest ⇒ for {
      b ← rest
      init = (a -> b)
      nps = allPairCombinations(rest filter (b !=))
      ps ← nps.isEmpty.fold(List(List(init)), nps map (np ⇒ init :: np))
    } yield ps
    case _ ⇒ Nil
  }

}
