package lila

import lila.socket.WithSocket

package object tournament extends PackageObject with WithPlay with WithSocket {

  private[tournament]type Players = List[tournament.Player]

  private[tournament]type RankedPlayers = List[RankedPlayer]

  private[tournament]type Pairings = List[tournament.Pairing]

  private[tournament]type Ranking = Map[String, Int]

  private[tournament]type Waiting = Map[String, Int]
}

package tournament {

case class RankedPlayer(rank: Int, player: Player) {
  def is(other: RankedPlayer) = player is other.player
  override def toString = s"$rank. ${player.userId}[${player.rating}]"
}

case class Winner(tourId: String, tourName: String, userId: String)
}
