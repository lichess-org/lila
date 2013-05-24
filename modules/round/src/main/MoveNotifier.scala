package lila.round

import akka.actor.ActorRef

import lila.hub.actorApi.monitor.AddMove
import lila.socket.actorApi.Fen

private[round] final class MoveNotifier(
    hub: ActorRef,
    monitor: lila.hub.ActorLazyRef) {

  def apply(gameId: String, fen: String, lastMove: Option[String]) {
    hub ! Fen(gameId, fen, lastMove)
    monitor ! AddMove
  }
}
