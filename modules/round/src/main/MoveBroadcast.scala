package lila.round

import akka.actor._

import lila.hub.actorApi.round.MoveEvent

private final class MoveBroadcast extends Actor {

  private val (enumerator, channel) =
    play.api.libs.iteratee.Concurrent.broadcast[MoveEvent]

  context.system.lilaBus.subscribe(self, 'moveEvent)

  override def postStop() {
    context.system.lilaBus.unsubscribe(self)
  }

  def receive = {

    case MoveBroadcast.GetEnumerator ⇒ sender ! enumerator

    case move: MoveEvent             ⇒ channel push move
  }
}

object MoveBroadcast {

  case object GetEnumerator
}
