package lila.round

import akka.actor._
import lila.hub.actorApi.game.ChangeFeatured
import lila.game.actorApi.MoveGameEvent
import lila.socket.Socket.makeMessage
import play.api.libs.iteratee._
import play.api.libs.json._

private final class TvBroadcast extends Actor {

  private val (enumerator, channel) = Concurrent.broadcast[JsValue]

  private var featuredId = none[String]

  bus.subscribe(self, 'changeFeaturedGame)

  def receive = {

    case TvBroadcast.GetEnumerator => sender ! enumerator

    case ChangeFeatured(id, msg) =>
      featuredId foreach { previous =>
        bus.unsubscribe(self, MoveGameEvent makeSymbol previous)
      }
      bus.subscribe(self, MoveGameEvent makeSymbol id)
      featuredId = id.some
      channel push msg

    case MoveGameEvent(_, fen, move) =>
      channel push makeMessage("fen", Json.obj(
        "fen" -> fen,
        "lm" -> move
      ))
  }

  private def bus = context.system.lilaBus
}

object TvBroadcast {

  type EnumeratorType = Enumerator[JsValue]

  case object GetEnumerator
}
