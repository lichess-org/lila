package lila.tournament
package arena

import lila.common.WMMatching
import PairingSystem.Data

private object AntmaPairing {

  def apply(data: Data, players: RankedPlayers): List[Pairing.Prep] = players.nonEmpty ?? {
    import data._

    val maxRank = players.map(_.rank).max

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
    def rankFactor(rank: Int) =
      300 + 1700 * (maxRank - rank) / maxRank

    def justPlayedTogether(u1: String, u2: String) =
      lastOpponents.hash.get(u1).contains(u2) ||
        lastOpponents.hash.get(u2).contains(u1)

    def pairScore(a: RankedPlayer, b: RankedPlayer): Option[Int] =
      if (justPlayedTogether(a.player.userId, b.player.userId)) None
      else Some {
        Math.abs(a.rank - b.rank) * rankFactor(Math.max(a.rank, b.rank)) +
          Math.abs(a.player.rating - b.player.rating)
      }

    WMMatching(players.toArray, pairScore).fold(
      err => {
        logger.error("WMMatching", err)
        Nil
      },
      _ map {
        case (a, b) => Pairing.prep(tour, a.player, b.player)
      }
    )
  }
}
