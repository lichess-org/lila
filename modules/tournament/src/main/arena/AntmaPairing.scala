package lila.tournament
package arena

import PairingSystem.{ Data, url }

private object AntmaPairing {

  def apply(data: Data, players: RankedPlayers): List[Pairing.Prep] = players.nonEmpty ?? {
    import data._

    val playersArray: Array[RankedPlayer] = players.toArray

    def playedTogether(u1: String, u2: String) =
      if (lastOpponents.hash.get(u1).contains(u2)) 1 else 0

    def f(x: Int): Int = (11500000 - 3500000 * x) * x

    def pairScore(i: Int, j: Int): Int = {
      val a = playersArray(i)
      val b = playersArray(j)
      Math.abs(a.rank - b.rank) * 1000 +
        Math.abs(a.player.rating - b.player.rating) +
        f {
          playedTogether(a.player.userId, b.player.userId) +
            playedTogether(b.player.userId, a.player.userId)
        }
    }

    try {
      val mate = WMMatching.minWeightMatching(WMMatching.fullGraph(playersArray.length, pairScore))
      WMMatching.mateToEdges(mate).map {
        case (i, j) => Pairing.prep(
          tour,
          playersArray(i).player,
          playersArray(j).player)
      }
    }
    catch {
      case e: Exception =>
        logger.error("AntmaPairing", e)
        Nil
    }
  }
}
