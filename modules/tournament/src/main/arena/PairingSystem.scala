package lila.tournament
package arena

import lila.user.{ User, UserRepo }

import scala.util.Random

final private[tournament] class PairingSystem(
    pairingRepo: PairingRepo,
    playerRepo: PlayerRepo,
    userRepo: UserRepo
)(implicit
    ec: scala.concurrent.ExecutionContext,
    idGenerator: lila.game.IdGenerator
) {

  import PairingSystem._
  import lila.tournament.Tournament.tournamentUrl

  // if waiting users can make pairings
  // then pair all users
  def createPairings(
      tour: Tournament,
      users: WaitingUsers,
      ranking: Ranking,
      smallTourNbActivePlayers: Option[Int]
  ): Fu[Pairings] = {
    for {
      lastOpponents <- pairingRepo.lastOpponents(tour.id, users.all, Math.min(300, users.size * 4))
      onlyTwoActivePlayers = smallTourNbActivePlayers.exists(2 ==)
      data                 = Data(tour, lastOpponents, ranking, onlyTwoActivePlayers)
      preps    <- evenOrAll(data, users)
      pairings <- prepsToPairings(preps)
    } yield pairings
  }.chronometer
    .logIfSlow(500, pairingLogger) { pairings =>
      s"createPairings ${tournamentUrl(tour.id)} ${pairings.size} pairings"
    }
    .result

  private def evenOrAll(data: Data, users: WaitingUsers) =
    makePreps(data, users.evenNumber) flatMap {
      case Nil if users.isOdd => makePreps(data, users.all)
      case x                  => fuccess(x)
    }

  private val maxGroupSize = 100

  private def makePreps(data: Data, users: Set[User.ID]): Fu[List[Pairing.Prep]] = {
    import data._
    if (users.sizeIs < 2) fuccess(Nil)
    else
      playerRepo.rankedByTourAndUserIds(tour.id, users, ranking) map { idles =>
        val nbIdles = idles.size
        if (data.tour.isRecentlyStarted && !data.tour.isTeamBattle) proximityPairings(tour, idles)
        else if (nbIdles > maxGroupSize) {
          // make sure groupSize is even with / 4 * 2
          val groupSize = (nbIdles / 4 * 2) atMost maxGroupSize
          bestPairings(data, idles take groupSize) :::
            bestPairings(data, idles drop groupSize take groupSize)
        } else if (nbIdles > 1) bestPairings(data, idles)
        else Nil
      }
  }.monSuccess(_.tournament.pairing.prep)
    .chronometer
    .logIfSlow(200, pairingLogger) { preps =>
      s"makePreps ${tournamentUrl(data.tour.id)} ${users.size} users, ${preps.size} preps"
    }
    .result

  private def prepsToPairings(preps: List[Pairing.Prep]): Fu[List[Pairing]] =
    idGenerator.games(preps.size) flatMap { ids =>
      if (preps.sizeCompare(30) <= 0)
        preps
          .zip(ids)
          .map { case (prep, id) =>
            userRepo.firstGetsSente(prep.user1.some, prep.user2.some) dmap prep.toPairing(id)
          }
          .sequenceFu
      else
        fuccess {
          preps.zip(ids).map { case (prep, id) =>
            prep.toPairing(id)(Random.nextBoolean())
          }
        }
    }

  private def proximityPairings(tour: Tournament, players: RankedPlayers): List[Pairing.Prep] =
    players grouped 2 collect { case List(p1, p2) =>
      Pairing.prep(tour, p1.player, p2.player)
    } toList

  private def bestPairings(data: Data, players: RankedPlayers): List[Pairing.Prep] =
    (players.sizeIs > 1) ?? AntmaPairing(data, players)
}

private object PairingSystem {

  case class Data(
      tour: Tournament,
      lastOpponents: Pairing.LastOpponents,
      ranking: Map[User.ID, Int],
      onlyTwoActivePlayers: Boolean
  )

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
  def rankFactorFor(players: RankedPlayers): (RankedPlayer, RankedPlayer) => Int = {
    val maxRank = players.maxBy(_.rank).rank
    (a, b) => {
      val rank = Math.min(a.rank, b.rank)
      300 + 1700 * (maxRank - rank) / maxRank
    }
  }
}
