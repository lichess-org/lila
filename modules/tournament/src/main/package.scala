package lila

import lila.socket.WithSocket

package object tournament extends PackageObject with WithSocket {

  private[tournament] type SocketMap = lila.hub.TrouperMap[tournament.TournamentSocket]

  private[tournament] type Players = List[tournament.Player]

  private[tournament] type RankedPlayers = List[RankedPlayer]

  private[tournament] type Pairings = List[tournament.Pairing]

  private[tournament] type Ranking = Map[lila.user.User.ID, Int]

  private[tournament] type Waiting = Map[lila.user.User.ID, Int]

  private[tournament] val logger = lila.log("tournament")

  private[tournament] val pairingLogger = logger branch "pairing"
}
