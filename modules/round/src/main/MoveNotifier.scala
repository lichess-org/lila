package lila.round

import lila.socket.actorApi.Fen
import lila.hub.actorApi.monitor.AddMove

import akka.actor.ActorRef

private[round] final class MoveNotifier(
    hub: ActorRef,
    monitor: lila.hub.ActorLazyRef) {

  def apply(gameId: String, fen: String, lastMove: Option[String]) {
    hub ! Fen(gameId, fen, lastMove)
    monitor ! AddMove
  }
}
