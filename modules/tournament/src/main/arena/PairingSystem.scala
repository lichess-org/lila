package lila.tournament
package arena

import lila.tournament.{ PairingSystem => AbstractPairingSystem }

import scala.concurrent.Future
import scala.util.Random

object PairingSystem extends AbstractPairingSystem {
  type P = (String, String)

  // if waiting users can make pairings
  // then pair all users
  def createPairings(tour: Tournament, users: AllUserIds): Future[(Pairings, Events)] =
    tryPairings(tour, users.waiting) flatMap {
      case nope@(Nil, _) if tour.pairings.isEmpty => tryPairings(tour, users.all)
      case nope@(Nil, _)                          => fuccess(nope)
      case _                                      => tryPairings(tour, users.all)
    }

  private def tryPairings(tour: Tournament, users: List[String]): Future[(Pairings, Events)] = {
    val nbActiveUsers = tour.nbActiveUsers

    if (users.size < 2)
      Future.successful((Nil, Nil))
    else {
      val idles: RankedPlayers = users.toSet.diff {
        tour.playingUserIds.toSet
      }.toList flatMap tour.rankedPlayerByUserId

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

  private def naivePairings(players: RankedPlayers) =
    players.sortBy { p =>
      p.rank -> -p.player.rating
    } grouped 2 collect {
      case List(p1, p2) if Random.nextBoolean => Pairing(p1.player, p2.player)
      case List(p1, p2)                       => Pairing(p2.player, p1.player)
    } toList

  private def smartPairings(players: RankedPlayers, pairings: Pairings, nbActiveUsers: Int): Pairings = {

    def lastOpponent(user: String): Option[String] =
      pairings find (_ contains user) flatMap (_ opponentOf user)

    def justPlayedTogether(u1: String, u2: String): Boolean =
      lastOpponent(u1).exists(u2==) || lastOpponent(u2).exists(u1==)

    // lower is better
    def score(pair: (RankedPlayer, RankedPlayer)): Int = pair match {
      case (a, b) if justPlayedTogether(a.player.id, b.player.id) => 9000
      case (a, b) => Math.abs(a.rank - b.rank) +
        Math.abs(a.player.rating - b.player.rating) / 1000
    }

    (players match {
      case x if x.size < 2 => Nil
      case List(p1, p2) if nbActiveUsers == 2 => List(p1.player -> p2.player)
      case List(p1, p2) if justPlayedTogether(p1.player.id, p2.player.id) => Nil
      case List(p1, p2) => List(p1.player -> p2.player)
      case ps => allPairCombinations(Random shuffle ps)
        .map(c => c -> c.map(score).sum)
        .sortBy(_._2)
        .headOption
        .??(_._1)
        .map {
          case (rp0, rp1) if Random.nextBoolean => rp0.player -> rp1.player
          case (rp0, rp1)                       => rp1.player -> rp0.player
        }
    }) map Pairing.apply
  }

  private def allPairCombinations(list: RankedPlayers): List[List[(RankedPlayer, RankedPlayer)]] = list match {
    case a :: rest => for {
      b ← rest
      init = (a -> b)
      nps = allPairCombinations(rest filterNot b.is)
      ps ← nps.isEmpty.fold(List(List(init)), nps map (np => init :: np))
    } yield ps
    case _ => Nil
  }
}
