package lila.round

import akka.actor.*
import akka.stream.scaladsl.*
import chess.format.Fen
import play.api.libs.json.*

import lila.common.{ Bus, LightUser }
import lila.common.Json.given
import lila.game.actorApi.MoveGameEvent
import lila.socket.Socket

final private class TvBroadcast(
    lightUserSync: LightUser.GetterSync
) extends Actor:

  import TvBroadcast.*

  private var clients = Set.empty[Client]

  private var featured = none[Featured]

  Bus.subscribe(self, "changeFeaturedGame")

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
          featured ifFalse compat foreach { f =>
            client.queue.offer(Socket.makeMessage("featured", f.dataWithFen))
          }
        }

    case Add(client)    => clients = clients + client
    case Remove(client) => clients = clients - client

    case ChangeFeatured(pov, msg) =>
      unsubscribeFromFeaturedId()
      Bus.subscribe(self, MoveGameEvent makeChan pov.gameId)
      val feat = Featured(
        pov.gameId,
        Json.obj(
          "id"          -> pov.gameId,
          "orientation" -> pov.color.name,
          "players" -> pov.game.players.mapList { p =>
            val user = p.userId.flatMap(lightUserSync)
            Json
              .obj("color" -> p.color.name)
              .add("user" -> user.map(LightUser.lightUserWrites.writes))
              .add("ai" -> p.aiLevel)
              .add("rating" -> p.rating)
              .add("seconds" -> pov.game.clock.map(_.remainingTime(pov.color).roundSeconds))
          }
        ),
        fen = Fen write pov.game.situation
      )
      clients.foreach { client =>
        client.queue offer {
          if client.fromLichess then msg
          else feat.socketMsg
        }
      }
      featured = feat.some

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
      clients.foreach(_.queue offer msg)
      featured foreach { f =>
        featured = f.copy(fen = fen).some
      }

  def unsubscribeFromFeaturedId() =
    featured foreach { previous =>
      Bus.unsubscribe(self, MoveGameEvent makeChan previous.id)
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
