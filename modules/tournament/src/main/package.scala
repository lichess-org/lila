package lidraughts

import lidraughts.socket.WithSocket

package object tournament extends PackageObject with WithSocket {

  private[tournament] type Players = List[tournament.Player]

  private[tournament] type RankedPlayers = List[RankedPlayer]

  private[tournament] type Pairings = List[tournament.Pairing]

  private[tournament] type Ranking = Map[lidraughts.user.User.ID, Int]

  private[tournament] type Waiting = Map[lidraughts.user.User.ID, Int]

  private[tournament] val logger = lidraughts.log("tournament")

  private[tournament] val pairingLogger = logger branch "pairing"
}
