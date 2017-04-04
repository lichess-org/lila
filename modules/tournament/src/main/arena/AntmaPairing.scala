package lila.tournament
package arena

import lila.common.WMMatching
import PairingSystem.Data

private object AntmaPairing {

  def apply(data: Data, players: RankedPlayers): List[Pairing.Prep] = players.nonEmpty ?? {
    import data._

    def rankFactor = PairingSystem.rankFactorFor(players)

    def justPlayedTogether(u1: String, u2: String) =
      lastOpponents.hash.get(u1).contains(u2) ||
        lastOpponents.hash.get(u2).contains(u1)

    def pairScore(a: RankedPlayer, b: RankedPlayer): Option[Int] =
      if (justPlayedTogether(a.player.userId, b.player.userId)) None
      else Some {
        Math.abs(a.rank - b.rank) * rankFactor(a, b) +
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
