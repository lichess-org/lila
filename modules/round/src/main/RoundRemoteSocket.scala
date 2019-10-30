package lila.round

import scala.concurrent.duration._

import actorApi.EventList
import lila.chat.Chat
import lila.common.Bus
import lila.game.{ Game, Event }
import lila.hub.{ Trouper, TrouperMap }
import lila.room.RoomSocket.{ Protocol => RP, _ }
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.{ Sri, SocketVersion, GetVersion, makeMessage }

final class RoundRemoteSocket(
    remoteSocketApi: lila.socket.RemoteSocket,
    chat: akka.actor.ActorSelection,
    messenger: Messenger,
    bus: Bus
) {

  import RoundRemoteSocket._

  private lazy val rooms = new TrouperMap(
    mkTrouper = roomId => new RoomState(RoomId(roomId), send, bus),
    accessTimeout = 5 minutes
  )

  def publish(game: Game, events: List[Event]): Unit =
    rooms.tell(game.id, EventList(events))

  private lazy val roundHandler: Handler = {
    case RP.In.ChatSay(roomId, userId, msg) => messenger.watcher(roomId.value, userId, msg)
    case Protocol.In.TellRoundSri(gameId, P.In.TellSri(sri, user, tpe, o)) => tpe match {
      case t => logger.warn(s"Unhandled round socket message: $t")
    }
  }

  private lazy val send: String => Unit = remoteSocketApi.makeSender("round-out").apply _

  remoteSocketApi.subscribe("round-in", Protocol.In.reader)(
    roundHandler orElse remoteSocketApi.baseHandler
  )
}

object RoundRemoteSocket {

  def appliesTo(game: Game) = game.casual

  private final class RoomState(roomId: RoomId, send: Send, bus: Bus) extends Trouper {

    private val chatId = Chat.Id(roomId.value)
    private def chatClassifier = Chat classify chatId
    private var version = SocketVersion(0)

    private def notifyVersion(nv: NotifyVersion[_]): Unit = {
      version = version.inc
      send(RP.Out.tellRoomVersion(roomId, nv.msg, version, nv.troll))
    }

    val process: Trouper.Receive = {
      case GetVersion(promise) => promise success version
      case nv: NotifyVersion[_] =>
        version = version.inc
        send(RP.Out.tellRoomVersion(roomId, nv.msg, version, nv.troll))
      case EventList(events) => events map { e =>
        version = version.inc
        send(RP.Out.tellRoomVersion(roomId, makeMessage(e.typ, e.data), version, e.troll))
      }
      // case lila.chat.actorApi.ChatLine(_, line) => line match {
      //   case line: UserLine => this ! NotifyVersion("message", lila.chat.JsonView(line), line.troll)
      //   case _ =>
      // }
      // case chatApi.OnTimeout(username) =>
      //   this ! NotifyVersion("chat_timeout", username, false)
      // case chatApi.OnReinstate(userId) =>
      //   this ! NotifyVersion("chat_reinstate", userId, false)
    }
    override def stop() {
      super.stop()
      send(RP.Out.stop(roomId))
      bus.unsubscribe(this, chatClassifier)
    }
    send(RP.Out.start(roomId))
    bus.subscribe(this, chatClassifier)
  }

  object Protocol {

    object In {

      case class TellRoundSri(gameId: Game.ID, tellSri: P.In.TellSri) extends P.In

      val reader: P.In.Reader = raw => roundReader(raw) orElse RP.In.reader(raw)

      val roundReader: P.In.Reader = raw => raw.path match {
        case "tell/round/sri" => raw.get(4) {
          case arr @ Array(gameId, _, _, _) => P.In.tellSriMapper.lift(arr drop 1).flatten map {
            TellRoundSri(gameId, _)
          }
        }
        case _ => none
      }
    }
  }
}
