package lila

import lila.game.Event
import lila.socket.WithSocket

package object round extends PackageObject with WithSocket {

  private[round] type SocketMap = lila.hub.TrouperMap[RoundSocket]

  private[round] type Events = List[Event]

  private[round] def logger = lila.log("round")
}

package round {

  private[round] sealed trait BenignError extends lila.base.LilaException
  private[round] case class ClientError(message: String) extends BenignError
  private[round] case class FishnetError(message: String) extends BenignError

  sealed trait OnTv

  case class OnLichessTv(channel: String, flip: Boolean) extends OnTv
  case class OnUserTv(userId: String) extends OnTv
}
