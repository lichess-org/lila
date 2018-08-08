package lidraughts.round

import akka.actor._
import lidraughts.hub.actorApi.game.ChangeFeatured
import lidraughts.hub.actorApi.round.MoveEvent
import lidraughts.socket.Socket.makeMessage
import play.api.libs.iteratee._
import play.api.libs.json._

private final class TvBroadcast extends Actor {

  private val (enumerator, channel) = Concurrent.broadcast[JsValue]

  private var featuredId = none[String]

  def receive = {

    case TvBroadcast.GetEnumerator => sender ! enumerator

    case ChangeFeatured(id, msg) =>
      featuredId = id.some
      channel push msg

    case move: MoveEvent if featuredId.contains(move.gameId) =>
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
