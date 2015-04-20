package lila.tournament
package arena

import lila.tournament.{ PairingSystem => AbstractPairingSystem }

import scala.concurrent.Future
import scala.util.Random

object PairingSystem extends AbstractPairingSystem {
  type P = (String, String)

  def createPairings(tour: Tournament, users: List[String]): Future[(Pairings, Events)] = {
    val nbActiveUsers = tour.nbActiveUsers

    if (users.size < 2)
      Future.successful((Nil, Nil))
    else {
      val idles: Players = users.toSet.diff {
        tour.playingUserIds.toSet
      }.toList flatMap tour.playerByUserId

      val ps = tour.pairings.isEmpty.fold(
        naivePairings(idles),
        (idles.size > 12).fold(
          naivePairings(idles),
          smartPairings(idles, tour.pairings, nbActiveUsers)
        )
      )

      Future.successful((ps, Nil))
    }
  }

  private def naivePairings(players: Players) =
    players.sortBy(_.rating) grouped 2 collect {
      case List(p1, p2) => Pairing(p1, p2)
    } toList

  private def smartPairings(players: Players, pairings: Pairings, nbActiveUsers: Int): Pairings = {

    def lastOpponent(user: String): Option[String] =
      pairings find (_ contains user) flatMap (_ opponentOf user)

    def justPlayedTogether(u1: String, u2: String): Boolean =
      lastOpponent(u1) == u2.some && lastOpponent(u2) == u1.some

    def timeSincePlay(u: String): Int =
      pairings.takeWhile(_ notContains u).size

    // lower is better
    def score(pair: (Player, Player)): Int = pair match {
      case (a, b) if justPlayedTogether(a.id, b.id) => 9000
      case (a, b)                                   => Math.abs(b.rating - a.rating)
    }

    (players match {
      case x if x.size < 2                                  => Nil
      case List(p1, p2) if nbActiveUsers == 2               => List(p1 -> p2)
      case List(p1, p2) if justPlayedTogether(p1.id, p2.id) => Nil
      case List(p1, p2)                                     => List(p1 -> p2)
      case ps => allPairCombinations(Random shuffle ps)
        .map(c => c -> c.map(score).sum)
        .sortBy(_._2)
        .headOption
        .map(_._1) | Nil
    }) map Pairing.apply
  }

  private def allPairCombinations(list: Players): List[List[(Player, Player)]] = list match {
    case a :: rest => for {
      b ← rest
      init = (a -> b)
      nps = allPairCombinations(rest filterNot b.is)
      ps ← nps.isEmpty.fold(List(List(init)), nps map (np => init :: np))
    } yield ps
    case _ => Nil
  }
}
