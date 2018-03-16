package lila.tournament
package arena

import lila.tournament.{ PairingSystem => AbstractPairingSystem }
import lila.user.UserRepo

import scala.util.Random

private[tournament] object PairingSystem extends AbstractPairingSystem {
  type P = (String, String)

  case class Data(
      tour: Tournament,
      lastOpponents: Pairing.LastOpponents,
      ranking: Map[String, Int],
      onlyTwoActivePlayers: Boolean
  ) {

    val isFirstRound = lastOpponents.hash.isEmpty && tour.isRecentlyStarted
  }

  // if waiting users can make pairings
  // then pair all users
  def createPairings(tour: Tournament, users: WaitingUsers, ranking: Ranking): Fu[Pairings] = {
    for {
      lastOpponents <- PairingRepo.lastOpponents(tour.id, users.all, Math.min(120, users.size * 4))
      onlyTwoActivePlayers <- (tour.nbPlayers <= 20) ?? PlayerRepo.countActive(tour.id).map(2==)
      data = Data(tour, lastOpponents, ranking, onlyTwoActivePlayers)
      preps <- if (data.isFirstRound) evenOrAll(data, users)
      else makePreps(data, users.waiting) flatMap {
        case Nil => fuccess(Nil)
        case _ => evenOrAll(data, users)
      }
      pairings <- prepsToPairings(preps)
    } yield pairings
  }.chronometer.logIfSlow(500, pairingLogger) { pairings =>
    s"createPairings ${url(tour.id)} ${pairings.size} pairings"
  }.result

  private def evenOrAll(data: Data, users: WaitingUsers) =
    makePreps(data, users.evenNumber) flatMap {
      case Nil if users.isOdd => makePreps(data, users.all)
      case x => fuccess(x)
    }

  private val maxGroupSize = 44

  private def makePreps(data: Data, users: List[String]): Fu[List[Pairing.Prep]] = {
    import data._
    if (users.size < 2) fuccess(Nil)
    else PlayerRepo.rankedByTourAndUserIds(tour.id, users, ranking) map { idles =>
      if (data.tour.isRecentlyStarted) naivePairings(tour, idles)
      else if (idles.size > maxGroupSize) {
        // make sure groupSize is even with / 4 * 2
        val groupSize = (idles.size / 4 * 2) atMost maxGroupSize
        smartPairings(data, idles take groupSize) :::
          smartPairings(data, idles drop groupSize take groupSize)
      } else if (idles.size > 1) smartPairings(data, idles)
      else Nil
    }
  }.chronometer.mon(_.tournament.pairing.prepTime).logIfSlow(200, pairingLogger) { preps =>
    s"makePreps ${url(data.tour.id)} ${users.size} users, ${preps.size} preps"
  }.result

  private def prepsToPairings(preps: List[Pairing.Prep]): Fu[List[Pairing]] =
    if (preps.size < 50) preps.map { prep =>
      UserRepo.firstGetsWhite(prep.user1.some, prep.user2.some) map prep.toPairing
    }.sequenceFu
    else fuccess {
      preps.map(_ toPairing Random.nextBoolean)
    }

  private def naivePairings(tour: Tournament, players: RankedPlayers): List[Pairing.Prep] =
    players grouped 2 collect {
      case List(p1, p2) => Pairing.prep(tour, p1.player, p2.player)
    } toList

  private def smartPairings(data: Data, players: RankedPlayers): List[Pairing.Prep] = players.size match {
    case x if x < 2 => Nil
    case x if x <= 10 => OrnicarPairing(data, players)
    case _ => AntmaPairing(data, players)
  }

  private[arena] def url(tourId: String) = s"https://lichess.org/tournament/$tourId"

  /* Was previously static 1000.
   * By increasing the factor for high ranked players,
   * we increase pairing quality for them.
   * The higher ranked, and the more ranking is relevant.
   * For instance rank 1 vs rank 5
   * is better thank 300 vs rank 310
   * This should increase leader vs leader pairing chances
   *
   * top rank factor = 2000
   * bottom rank factor = 300
   */
  private[arena] def rankFactorFor(players: RankedPlayers): (RankedPlayer, RankedPlayer) => Int = {
    val maxRank = players.map(_.rank).max
    (a, b) => {
      val rank = Math.min(a.rank, b.rank)
      300 + 1700 * (maxRank - rank) / maxRank
    }
  }
}
