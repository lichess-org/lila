package lila.round

import akka.actor._
import akka.stream.scaladsl._
import lila.hub.actorApi.game.ChangeFeatured
import lila.hub.actorApi.round.MoveEvent
import lila.socket.Socket.makeMessage
import play.api.libs.json._

private final class TvBroadcast extends Actor {

  import TvBroadcast._
  implicit val mat = context.system.lilaMat

  var featuredId = none[String]

  val clients = scala.collection.mutable.Set.empty[ActorRef]

  def receive = {

    case GetSource =>
      val (client, source) = lila.common.AkkaStream.actorSource[JsValue](10)
      context.watch(client)
      clients += client
      sender ! source

    case ChangeFeatured(id, msg) =>
      featuredId = id.some
      sendToClients(msg)

    case move: MoveEvent if Some(move.gameId) == featuredId =>
      sendToClients(makeMessage("fen", Json.obj(
        "fen" -> move.fen,
        "lm" -> move.move
      )))

    case Terminated(client) =>
      context.unwatch(client)
      clients -= client
  }

  def sendToClients(json: JsValue) = {
    clients.foreach { _ ! json }
  }
}

object TvBroadcast {

  type SourceType = Source[JsValue, akka.NotUsed]

  case object GetSource
}
