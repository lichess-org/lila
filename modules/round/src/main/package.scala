package lila

import lila.game.Event
import lila.socket.WithSocket

package object round extends PackageObject with WithPlay with WithSocket {

  private[round]type Events = List[Event]

  private[round]type VersionedEvents = List[VersionedEvent]
}

package round {

private[round] sealed trait BenignError extends lila.common.LilaException
private[round] case class ClientError(message: String) extends BenignError
private[round] case class FishnetError(message: String) extends BenignError

case class OnTv(channel: String, flip: Boolean)
}
