package lila.round

import akka.actor._
import lila.hub.actorApi.game.ChangeFeatured
import lila.hub.actorApi.round.MoveEvent
import lila.socket.Socket.makeMessage
import play.api.libs.iteratee._
import play.api.libs.json._

private final class TvBroadcast extends Actor {

  context.system.lilaBus.subscribe(self, 'moveEvent, 'changeFeaturedGame)

  override def postStop() {
    context.system.lilaBus.unsubscribe(self)
  }

  private val (enumerator, channel) = Concurrent.broadcast[JsValue]

  private var featuredId = none[String]

  def receive = {

    case TvBroadcast.GetEnumerator => sender ! enumerator

    case ChangeFeatured(id, html) =>
      featuredId = id.some
      channel push makeMessage("featured", Json.obj("html" -> html.toString))

    case move: MoveEvent if Some(move.gameId) == featuredId =>
      channel push makeMessage("fen", Json.obj(
        "fen" -> move.fen,
        "lm" -> move.move
      ))
  }
}

object TvBroadcast {

  type EnumeratorType = Enumerator[JsValue]

  case object GetEnumerator
}
