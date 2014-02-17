package lila.round

import akka.actor._

import lila.hub.actorApi.round.MoveEvent
import play.api.libs.iteratee._

private final class MoveBroadcast extends Actor {

  context.system.lilaBus.subscribe(self, 'moveEvent)

  override def postStop() {
    context.system.lilaBus.unsubscribe(self)
  }

  val format = Enumeratee.map[MoveEvent] { move =>
    s"${move.gameId} ${move.move}${move.meta} ${move.ip}"
  }

  val (enumerator, channel) =
    play.api.libs.iteratee.Concurrent.broadcast[MoveEvent]

  val formattedEnumerator = enumerator &> format

  def receive = {

    case MoveBroadcast.GetEnumerator => sender ! formattedEnumerator

    case move: MoveEvent             => channel push move
  }
}

object MoveBroadcast {

  case object GetEnumerator
}
