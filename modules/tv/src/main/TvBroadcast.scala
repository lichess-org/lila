package lila.tv

import akka.actor.*
import akka.stream.scaladsl.*
import chess.format.Fen
import lila.Lila.{ GameId, none }
import lila.common.Json.given
import lila.common.{ Bus, LightUser }
import lila.game.Pov
import lila.game.actorApi.MoveGameEvent
import lila.round.actorApi.TvSelect
import lila.socket.Socket
import play.api.libs.json.*

final private class TvBroadcast(
    lightUserSync: LightUser.GetterSync,
    channel: Tv.Channel,
    gameProxyRepo: lila.round.GameProxyRepo
) extends Actor:

  import TvBroadcast.*

  private var clients = Set.empty[Client]

  private var featured = none[Featured]

  Bus.subscribe(self, "tvSelect")

  given Executor = context.system.dispatcher

  override def postStop() =
    super.postStop()
    unsubscribeFromFeaturedId()

  def receive =

    case TvBroadcast.Connect(compat) =>
      sender() ! Source
        .queue[JsValue](8, akka.stream.OverflowStrategy.dropHead)
        .mapMaterializedValue { queue =>
          val client = Client(queue, compat)
          self ! Add(client)
          queue.watchCompletion().addEffectAnyway {
            self ! Remove(client)
          }
          featured.ifFalse(compat).foreach { f =>
            client.queue.offer(Socket.makeMessage("featured", f.dataWithFen))
          }
        }

    case Add(client)    => clients = clients + client
    case Remove(client) => clients = clients - client

    case TvSelect(gameId, speed, chanKey, data) if chanKey == channel.key =>
      gameProxyRepo.game(gameId).map2 { game =>
        unsubscribeFromFeaturedId()
        Bus.subscribe(self, MoveGameEvent.makeChan(gameId))
        val pov = Pov.naturalOrientation(game)
        val feat = Featured(
          gameId,
          Json.obj(
            "id"          -> gameId,
            "orientation" -> pov.color.name,
            "players" -> game.players.mapList: p =>
              val user = p.userId.flatMap(lightUserSync)
              Json
                .obj("color" -> p.color.name)
                .add("user" -> user.map(LightUser.write))
                .add("ai" -> p.aiLevel)
                .add("rating" -> p.rating)
                .add("seconds" -> game.clock.map(_.remainingTime(pov.color).roundSeconds))
          ),
          fen = Fen.write(game.situation)
        )
        clients.foreach: client =>
          client.queue.offer:
            if client.fromLichess then data
            else feat.socketMsg
        featured = feat.some
      }

    case MoveGameEvent(game, fen, move) =>
      val msg = Socket.makeMessage(
        "fen",
        Json
          .obj(
            "fen" -> fen,
            "lm"  -> move
          )
          .add("wc" -> game.clock.map(_.remainingTime(chess.White).roundSeconds))
          .add("bc" -> game.clock.map(_.remainingTime(chess.Black).roundSeconds))
      )
      clients.foreach(_.queue.offer(msg))
      featured.foreach { f =>
        featured = f.copy(fen = fen).some
      }

  def unsubscribeFromFeaturedId() =
    featured.foreach { previous =>
      Bus.unsubscribe(self, MoveGameEvent.makeChan(previous.id))
    }

object TvBroadcast:

  type SourceType = Source[JsValue, ?]
  type Queue      = SourceQueueWithComplete[JsValue]

  case class Featured(id: GameId, data: JsObject, fen: Fen.Epd):
    def dataWithFen = data ++ Json.obj("fen" -> fen)
    def socketMsg   = Socket.makeMessage("featured", dataWithFen)

  case class Connect(fromLichess: Boolean)
  case class Client(queue: Queue, fromLichess: Boolean)

  case class Add(c: Client)
  case class Remove(c: Client)
