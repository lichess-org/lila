package lila.tournament
package arena

import lila.tournament.{ PairingSystem => AbstractPairingSystem }

import scala.util.Random
import scala.concurrent.Future

object PairingSystem extends AbstractPairingSystem {
  type P = (String, String)

  def createPairings(tour: Tournament, users: List[String]): Future[(Pairings,Events)] = {
    val pairings = tour.pairings
    val nbActiveUsers = tour.nbActiveUsers

    if (users.size < 2)
      Future.successful((Nil,Nil))
    else {
      val idles: List[String] = Random shuffle {
        users.toSet diff { (pairings filter (_.playing) flatMap (_.users)).toSet } toList
      }

      val ps = pairings.isEmpty.fold(
        naivePairings(idles),
        (idles.size > 12).fold(
          naivePairings(idles),
          smartPairings(idles, pairings, nbActiveUsers)
        )
      )

      Future.successful((ps,Nil))
    }
  }

  private def naivePairings(users: List[String]) =
    Random shuffle users grouped 2 collect {
      case List(u1, u2) => Pairing(u1, u2)
    } toList

  private def smartPairings(users: List[String], pairings: Pairings, nbActiveUsers: Int): Pairings = {

    def lastOpponent(user: String): Option[String] =
      pairings find (_ contains user) flatMap (_ opponentOf user)

    def justPlayedTogether(u1: String, u2: String): Boolean =
      lastOpponent(u1) == u2.some && lastOpponent(u2) == u1.some

    def timeSincePlay(u: String): Int =
      pairings.takeWhile(_ notContains u).size

    // lower is better
    def score(pair: P): Int = pair match {
      case (a, b) => justPlayedTogether(a, b).fold(
        100,
        -timeSincePlay(a) - timeSincePlay(b))
    }

    (users match {
      case x if x.size < 2                            => Nil
      case List(u1, u2) if nbActiveUsers == 2         => List(u1 -> u2)
      case List(u1, u2) if justPlayedTogether(u1, u2) => Nil
      case List(u1, u2)                               => List(u1 -> u2)
      case us => allPairCombinations(us)
        .map(c => c -> c.map(score).sum)
        .sortBy(_._2)
        .headOption
        .map(_._1) | Nil
    }) map Pairing.apply
  }

  private def allPairCombinations(list: List[String]): List[List[(String, String)]] = list match {
    case a :: rest => for {
      b ← rest
      init = (a -> b)
      nps = allPairCombinations(rest filter (b !=))
      ps ← nps.isEmpty.fold(List(List(init)), nps map (np => init :: np))
    } yield ps
    case _ => Nil
  }
}

