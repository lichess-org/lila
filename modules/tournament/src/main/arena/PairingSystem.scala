package lila.tournament
package arena

import lila.core.chess.Rank

final private[tournament] class PairingSystem(
    pairingRepo: PairingRepo,
    playerRepo: PlayerRepo,
    colorHistoryApi: ColorHistoryApi
)(using
    ec: Executor,
    idGenerator: lila.core.game.IdGenerator
):

  import PairingSystem.*
  import lila.tournament.Tournament.tournamentUrl

  // if waiting users can make pairings
  // then pair all users
  def createPairings(
      tour: Tournament,
      users: WaitingUsers,
      ranking: FullRanking,
      smallTourNbActivePlayers: Option[Int]
  ): Fu[List[Pairing.WithPlayers]] = {
    for
      lastOpponents <-
        if tour.isRecentlyStarted then fuccess(Pairing.LastOpponents(Map.empty))
        else pairingRepo.lastOpponents(tour.id, users.all, Math.min(300, users.size * 4))
      onlyTwoActivePlayers = smallTourNbActivePlayers.exists(2 ==)
      data = Data(tour, lastOpponents, ranking.ranking, onlyTwoActivePlayers)
      preps <- evenOrAll(data, users)
      pairings <- prepsToPairings(tour, preps)
    yield pairings
  }.chronometer
    .logIfSlow(500, pairingLogger) { pairings =>
      s"createPairings ${tournamentUrl(tour.id)} ${pairings.size} pairings"
    }
    .result

  private def evenOrAll(data: Data, users: WaitingUsers) =
    makePreps(data, users.evenNumber).flatMap:
      case Nil if users.isOdd => makePreps(data, users.all)
      case x => fuccess(x)

  private val maxGroupSize = 100

  private def makePreps(data: Data, users: Set[UserId]): Fu[List[Pairing.Prep]] = {
    import data.*
    if users.sizeIs < 2 then fuccess(Nil)
    else
      playerRepo.rankedByTourAndUserIds(tour.id, users, ranking).map { idles =>
        lazy val nbIdles = idles.size
        if nbIdles < 2 then Nil
        else if data.tour.isRecentlyStarted && !data.tour.isTeamBattle then initialPairings(idles)
        else if nbIdles <= maxGroupSize then bestPairings(data, idles)
        else
          // make sure groupSize is even with / 4 * 2
          val groupSize = (nbIdles / 4 * 2).atMost(maxGroupSize)
          // make 2 best pairing groups
          bestPairings(data, idles.take(groupSize)) :::
            bestPairings(data, idles.slice(groupSize, groupSize * 2)) :::
            // then up to 6 more groups of cheap pairing
            proximityPairings(idles.slice(groupSize * 2, groupSize * 8))
      }
  }.monSuccess(_.tournament.pairing.prep)
    .chronometer
    .logIfSlow(200, pairingLogger) { preps =>
      s"makePreps ${tournamentUrl(data.tour.id)} ${users.size} users, ${preps.size} preps"
    }
    .result

  private def prepsToPairings(tour: Tournament, preps: List[Pairing.Prep]): Fu[List[Pairing.WithPlayers]] =
    idGenerator.games(preps.size).map { ids =>
      preps.zip(ids).map { case (prep, id) =>
        // color was chosen in prepWithColor function
        prep.toPairing(tour.id, id)
      }
    }

  private def initialPairings(players: RankedPlayers): List[Pairing.Prep] =
    players
      .grouped(2)
      .collect:
        case List(p1, p2) => Pairing.prepWithRandomColor(p1.player, p2.player)
      .toList

  private def proximityPairings(players: RankedPlayers): List[Pairing.Prep] =
    addColorHistory(players)
      .grouped(2)
      .collect:
        case List(p1, p2) if !p1.sameTeamAs(p2) => Pairing.prepWithColor(p1, p2)
      .toList

  private def bestPairings(data: Data, players: RankedPlayers): List[Pairing.Prep] =
    (players.sizeIs > 1).so(AntmaPairing(data, addColorHistory(players)))

  private def addColorHistory(players: RankedPlayers) = players.map(_.withColorHistory(colorHistoryApi.get))

private object PairingSystem:

  case class Data(
      tour: Tournament,
      lastOpponents: Pairing.LastOpponents,
      ranking: Map[UserId, Rank],
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
  def rankFactorFor(
      players: List[RankedPlayerWithColorHistory]
  ): (RankedPlayerWithColorHistory, RankedPlayerWithColorHistory) => Int =
    val maxRank = players.maxBy(_.rank.value).rank
    (a, b) =>
      val rank = a.rank.atMost(b.rank)
      300 + 1700 * (maxRank - rank).value / maxRank.value
