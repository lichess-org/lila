package lila.swiss

import lila.db.dsl._

final private class SwissDirector(
    colls: SwissColls,
    pairingSystem: PairingSystem
)(
    implicit ec: scala.concurrent.ExecutionContext
) {
  import BsonHandlers._

  def apply(swiss: Swiss): Funit =
    for {
      players <- SwissPlayer.fields { f =>
        colls.player.ext
          .find($doc(f.swissId -> swiss.id))
          .sort($sort asc f.number)
          .list[SwissPlayer]()
      }
      prevPairings <- SwissPairing.fields { f =>
        colls.pairing.ext
          .find($doc(f.swissId -> swiss.id))
          .sort($sort asc f.round)
          .list[SwissPairing]()
      }
    } yield {
      val pairings = pairingSystem(swiss, players, prevPairings)
      println(pairings)
    }
}
