package lila.round

import lila.socket.actorApi.Fen
import lila.hub.actorApi.monitor.AddMove

import akka.actor.ActorRef
import play.api.libs.concurrent.Execution.Implicits._

private[round] final class MoveNotifier(
    hub: ActorRef,
    monitor: ActorRef) {

  def apply(gameId: String, fen: String, lastMove: Option[String]) {
    hub ! Fen(gameId, fen, lastMove)
    monitor ! AddMove
  }
}
