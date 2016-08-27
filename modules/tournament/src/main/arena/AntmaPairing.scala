package lila.tournament
package arena

import PairingSystem.{ Data, url }

private object AntmaPairing {

  def apply(data: Data, players: RankedPlayers): List[Pairing.Prep] = players.nonEmpty ?? {
    import data._

    val a: Array[RankedPlayer] = players.toArray
    val n: Int = a.length

    def playedTogether(u1: String, u2: String) =
      if (lastOpponents.hash.get(u1).contains(u2)) 1 else 0

    def f(x: Int): Int = (11500000 - 3500000 * x) * x

    def pairScore(i: Int, j: Int): Int =
      Math.abs(a(i).rank - a(j).rank) * 1000 +
        Math.abs(a(i).player.rating - a(j).player.rating) +
        f {
          playedTogether(a(i).player.userId, a(j).player.userId) +
            playedTogether(a(j).player.userId, a(i).player.userId)
        }

    try {
      val mate = WMMatching.minWeightMatching(WMMatching.fullGraph(n, pairScore))
      WMMatching.mateToEdges(mate).map { x =>
        Pairing.prep(tour, a(x._1).player, a(x._2).player)
      }
    }
    catch {
      case e: Exception =>
        logger.error("AntmaPairing", e)
        Nil
    }
  }
}
