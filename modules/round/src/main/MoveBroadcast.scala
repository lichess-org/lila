package lila.round

import akka.actor._
import akka.event.EventStream

import lila.hub.actorApi.round.MoveEvent

private final class MoveBroadcast extends Actor {

  private val (enumerator, channel) =
    play.api.libs.iteratee.Concurrent.broadcast[MoveEvent]

  context.system.eventStream.subscribe(self, classOf[MoveEvent])

  override def postStop() {
    context.system.eventStream.unsubscribe(self)
  }

  def receive = {

    case MoveBroadcast.GetEnumerator ⇒ sender ! enumerator

    case move: MoveEvent             ⇒ channel push move
  }
}

object MoveBroadcast {

  case object GetEnumerator
}
