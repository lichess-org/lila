package lila.round

import akka.actor._
import akka.stream.scaladsl._
import lila.game.actorApi.MoveGameEvent
import lila.hub.actorApi.game.ChangeFeatured
import lila.socket.Socket.makeMessage
import play.api.libs.json._

import lila.common.Bus

private final class TvBroadcast extends Actor {

  private var queues = Set.empty[SourceQueueWithComplete[JsValue]]

  private var featuredId = none[String]

  Bus.subscribe(self, "changeFeaturedGame")

  def receive = {

    case TvBroadcast.Connect =>
      sender ! Source.queue[JsValue](8, akka.stream.OverflowStrategy.dropHead)
        .mapMaterializedValue { queue =>
          queues = queues + queue
          queue.watchCompletion.foreach { _ => queues = queues - queue }
        }

    case ChangeFeatured(id, msg) =>
      featuredId foreach { previous =>
        Bus.unsubscribe(self, MoveGameEvent makeChan previous)
      }
      Bus.subscribe(self, MoveGameEvent makeChan id)
      featuredId = id.some
      queues.foreach(_ offer msg)

    case MoveGameEvent(_, fen, move) if queues.nonEmpty =>
      val msg = makeMessage("fen", Json.obj(
        "fen" -> fen,
        "lm" -> move
      ))
      queues.foreach(_ offer msg)
  }
}

object TvBroadcast {

  type SourceType = Source[JsValue, _]

  case object Connect
}
