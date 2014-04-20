package lila.round

import akka.actor._

import lila.hub.actorApi.round.MoveEvent
import play.api.libs.iteratee._

private final class MoveBroadcast extends Actor {

  context.system.lilaBus.subscribe(self, 'moveEvent)

  override def postStop() {
    context.system.lilaBus.unsubscribe(self)
  }

  private val (enumerator, channel) = Concurrent.broadcast[String]

  def receive = {

    case MoveBroadcast.GetEnumerator => sender ! enumerator

    case move: MoveEvent             => channel push s"${move.gameId} ${move.ip}"
  }
}

object MoveBroadcast {

  case object GetEnumerator
}
