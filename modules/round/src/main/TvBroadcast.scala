package lila.round

import akka.actor._
import akka.stream.scaladsl._
import lila.game.actorApi.MoveGameEvent
import lila.hub.actorApi.game.ChangeFeatured
import lila.socket.Socket.makeMessage
import play.api.libs.json._

import lila.common.Bus

final private class TvBroadcast extends Actor {

  import TvBroadcast._

  private var queues = Set.empty[Queue]

  private var featuredId = none[String]

  Bus.subscribe(self, "changeFeaturedGame")

  implicit def system = context.dispatcher

  override def postStop() = {
    super.postStop()
    unsubscribeFromFeaturedId()
  }

  def receive = {

    case TvBroadcast.Connect =>
      sender() ! Source
        .queue[JsValue](8, akka.stream.OverflowStrategy.dropHead)
        .mapMaterializedValue { queue =>
          self ! Add(queue)
          queue.watchCompletion().foreach { _ =>
            self ! Remove(queue)
          }
        }

    case Add(queue)    => queues = queues + queue
    case Remove(queue) => queues = queues - queue

    case ChangeFeatured(id, msg) =>
      unsubscribeFromFeaturedId()
      Bus.subscribe(self, MoveGameEvent makeChan id)
      featuredId = id.some
      queues.foreach(_ offer msg)

    case MoveGameEvent(game, fen, move) if queues.nonEmpty =>
      val msg = makeMessage(
        "fen",
        Json
          .obj(
            "fen" -> s"$fen ${game.turnColor.letter}",
            "lm"  -> move
          )
          .add("wc" -> game.clock.map(_.remainingTime(chess.White).roundSeconds))
          .add("bc" -> game.clock.map(_.remainingTime(chess.Black).roundSeconds))
      )
      queues.foreach(_ offer msg)
  }

  def unsubscribeFromFeaturedId() =
    featuredId foreach { previous =>
      Bus.unsubscribe(self, MoveGameEvent makeChan previous)
    }
}

object TvBroadcast {

  type SourceType = Source[JsValue, _]
  type Queue      = SourceQueueWithComplete[JsValue]

  case object Connect

  case class Add(q: Queue)
  case class Remove(q: Queue)
}
