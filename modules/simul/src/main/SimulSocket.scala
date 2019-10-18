package lila.simul

import akka.actor._
import play.api.libs.json._
import scala.concurrent.duration._

import actorApi._
import lila.chat.ChatTimeout.Reason
import lila.chat.{ Chat, UserLine, actorApi => chatApi }
import lila.game.{ Game, Pov }
import lila.hub.{ TrouperMap, Trouper }
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.{ makeMessage, GetVersion, SocketVersion }
import lila.user.User

private final class SimulSocket(
    getSimul: Simul.ID => Fu[Option[Simul]],
    jsonView: JsonView,
    remoteSocketApi: lila.socket.RemoteSocket,
    chat: ActorSelection,
    system: ActorSystem
) {

  private final class SimulState(roomId: P.RoomId) extends Trouper {
    private var version = SocketVersion(0)
    val process: Trouper.Receive = {
      case GetVersion(promise) => promise success version
      case nv: P.NotifyVersion[_] =>
        version = version.inc
        send(P.Out.tellVersion(roomId, nv.msg, version, nv.troll))
    }
    override def stop() {
      super.stop()
      send(P.Out.stop(roomId))
    }
    send(P.Out.start(roomId))
  }

  private val sockets = new TrouperMap(
    mkTrouper = simulId => new SimulState(P.RoomId(simulId)),
    accessTimeout = 5 minutes
  )

  def versionOf(simulId: Simul.ID): Fu[SocketVersion] =
    sockets.ask[SocketVersion](simulId)(GetVersion)

  def hostIsOn(simulId: Simul.ID, gameId: Game.ID): Unit =
    sockets.tell(simulId, P.NotifyVersion("hostGame", gameId))

  def reload(simulId: Simul.ID): Unit =
    getSimul(simulId) foreach {
      _ foreach { simul =>
        jsonView(simul, none) foreach { obj =>
          sockets.tell(simulId, P.NotifyVersion("reload", obj))
        }
      }
    }

  def aborted(simulId: Simul.ID): Unit =
    sockets.tell(simulId, P.NotifyVersion("aborted", Json.obj()))

  def startSimul(simul: Simul, firstGame: Game): Unit =
    firstGame.playerByUserId(simul.hostId) foreach { player =>
      redirectPlayer(simul, Pov(firstGame, player))
    }

  def startGame(simul: Simul, game: Game): Unit =
    game.playerByUserId(simul.hostId) foreach { opponent =>
      redirectPlayer(simul, Pov(game, !opponent.color))
    }

  private def redirectPlayer(simul: Simul, pov: Pov): Unit =
    pov.player.userId foreach { userId =>
      send(P.Out.tellRoomUser(P.RoomId(simul.id), userId, makeMessage("redirect", pov.fullId)))
    }

  private val handler: Handler = {
    case P.In.ChatSay(roomId, userId, msg) =>
      val publicSource = lila.hub.actorApi.shutup.PublicSource.Simul(roomId.value).some
      chat ! lila.chat.actorApi.UserTalk(Chat.Id(roomId.value), userId, msg, publicSource)
    case P.In.ChatTimeout(roomId, modId, suspect, reason) => lila.chat.ChatTimeout.Reason(reason) foreach { r =>
      chat ! lila.chat.actorApi.Timeout(Chat.Id(roomId.value), modId, suspect, r, local = false)
    }
    case P.In.DisconnectAll =>
      sockets.killAll
  }

  remoteSocketApi.subscribe("simul-in", P.In.baseReader)(handler orElse remoteSocketApi.baseHandler)

  private lazy val send: String => Unit = remoteSocketApi.makeSender("simul-out").apply _

  system.lilaBus.subscribeFun('remoteSocketChat) {
    case chatApi.ChatLine(chatId, line: UserLine) =>
      sockets.tell(chatId.value, P.NotifyVersion("message", lila.chat.JsonView(line), line.troll))
    case chatApi.OnTimeout(chatId, username) =>
      sockets.tell(chatId.value, P.NotifyVersion("chat_timeout", username, false))
    case chatApi.OnReinstate(chatId, userId) =>
      sockets.tell(chatId.value, P.NotifyVersion("chat_reinstate", userId, false))
    case a => println(s"remote socket chat unhandled $a")
  }
}
