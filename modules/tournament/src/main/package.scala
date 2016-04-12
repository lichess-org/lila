package lila

import lila.socket.WithSocket

package object tournament extends PackageObject with WithPlay with WithSocket {

  private[tournament]type Players = List[tournament.Player]

  private[tournament]type RankedPlayers = List[RankedPlayer]

  private[tournament]type Pairings = List[tournament.Pairing]

  private[tournament]type Ranking = Map[String, Int]

  private[tournament]type Waiting = Map[String, Int]

  private[tournament] val logger = lila.log("tournament")

  private[tournament] val pairingLogger = logger branch "pairing"
}
