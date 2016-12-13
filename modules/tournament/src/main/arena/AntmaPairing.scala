package lila.tournament
package arena

import lila.common.WMMatching
import PairingSystem.{ Data, url }

private object AntmaPairing {

  def apply(data: Data, players: RankedPlayers): List[Pairing.Prep] = players.nonEmpty ?? {
    import data._

    def playedTogether(u1: String, u2: String) =
      if (lastOpponents.hash.get(u1).contains(u2)) 1 else 0

    def f(x: Int): Int = (11500000 - 3500000 * x) * x

    def pairScore(a: RankedPlayer, b: RankedPlayer): Option[Int] = Some {
      Math.abs(a.rank - b.rank) * 1000 +
        Math.abs(a.player.rating - b.player.rating) +
        f {
          playedTogether(a.player.userId, b.player.userId) +
            playedTogether(b.player.userId, a.player.userId)
        }
    }

    WMMatching(players.toArray, pairScore).fold(
      err => {
        logger.error("WMMatching", err)
        Nil
      },
      _ map {
        case (a, b) => Pairing.prep(tour, a.player, b.player)
      })
  }
}
