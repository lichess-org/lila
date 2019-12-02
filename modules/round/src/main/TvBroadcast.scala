package lila.round

import akka.actor._
import lila.hub.actorApi.game.ChangeFeatured
import lila.game.actorApi.MoveGameEvent
import lila.socket.Socket.makeMessage
import play.api.libs.json._

import lila.common.Bus

// #TODO
private final class TvBroadcast extends Actor {

  private var featuredId = none[String]

  Bus.subscribe(self, "changeFeaturedGame")

  ???

  def receive = {

    case _ => ???

    // case TvBroadcast.GetEnumerator => sender ! enumerator

    // case ChangeFeatured(id, msg) =>
    //   featuredId foreach { previous =>
    //     Bus.unsubscribe(self, MoveGameEvent makeChan previous)
    //   }
    //   Bus.subscribe(self, MoveGameEvent makeChan id)
    //   featuredId = id.some
    //   channel push msg

    // case MoveGameEvent(_, fen, move) =>
    //   channel push makeMessage("fen", Json.obj(
    //     "fen" -> fen,
    //     "lm" -> move
    //   ))
  }
}

object TvBroadcast {

  case object GetEnumerator
}
