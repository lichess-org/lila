package lila.round

import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import lila.hub.actorApi.game.ChangeFeatured
import lila.hub.actorApi.round.MoveEvent
import lila.socket.Socket.makeMessage
import play.api.libs.json._

private final class TvBroadcast extends Actor {

  val (out, publisher) =
    lila.common.AkkaStream.actorPublisher(20, OverflowStrategy.dropHead)(materializer(context.system))

  private var featuredId = none[String]

  def receive = {

    case TvBroadcast.GetPublisher => sender ! publisher

    case ChangeFeatured(id, msg) =>
      featuredId = id.some
      out ! msg

    case move: MoveEvent if Some(move.gameId) == featuredId =>
      out ! makeMessage("fen", Json.obj(
        "fen" -> move.fen,
        "lm" -> move.move
      ))
  }
}

object TvBroadcast {

  type PublisherType = org.reactivestreams.Publisher[JsValue]

  case object GetPublisher
}
