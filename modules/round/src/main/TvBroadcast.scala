package lila.round

import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import lila.hub.actorApi.game.ChangeFeatured
import lila.hub.actorApi.round.MoveEvent
import lila.socket.Socket.makeMessage
import play.api.libs.json._

private final class TvBroadcast extends Actor {

  implicit val mat = materializer(context.system)

  val (actorRef, publisher) = Source.actorRef(20, OverflowStrategy.dropHead)
    .toMat(Sink asPublisher false)(Keep.both).run()

  private var featuredId = none[String]

  def receive = {

    case TvBroadcast.GetPublisher  => sender ! publisher

    case ChangeFeatured(id, msg) =>
      featuredId = id.some
      actorRef ! msg

    case move: MoveEvent if Some(move.gameId) == featuredId =>
      actorRef ! makeMessage("fen", Json.obj(
        "fen" -> move.fen,
        "lm" -> move.move
      ))
  }
}

object TvBroadcast {

  import org.reactivestreams.Publisher
  type PublisherType = Publisher[JsValue]
  case object GetPublisher
}
