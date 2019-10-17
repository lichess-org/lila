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

  import SimulSocket._

  private final class SimulState(simulId: Simul.ID) extends Trouper {
    private var version = SocketVersion(0)
    val process: Trouper.Receive = {
      case GetVersion(promise) => promise success version
      case nv: NotifyVersion[_] =>
        version = version.inc
        send(Protocol.Out.tellVersion(simulId, nv.msg, version, nv.troll))
    }
    override def stop() {
      super.stop()
      send(Protocol.Out.stop(simulId))
    }
    send(Protocol.Out.start(simulId))
  }

  private val sockets = new TrouperMap(
    mkTrouper = simulId => new SimulState(simulId),
    accessTimeout = 5 minutes
  )

  def versionOf(simulId: Simul.ID): Fu[SocketVersion] =
    sockets.ask[SocketVersion](simulId)(GetVersion)

  def hostIsOn(simulId: Simul.ID, gameId: Game.ID): Unit =
    sockets.tell(simulId, NotifyVersion("hostGame", gameId))

  def reload(simulId: Simul.ID): Unit =
    getSimul(simulId) foreach {
      _ foreach { simul =>
        jsonView(simul, none) foreach { obj =>
          sockets.tell(simulId, NotifyVersion("reload", obj))
        }
      }
    }

  def aborted(simulId: Simul.ID): Unit =
    sockets.tell(simulId, NotifyVersion("aborted", Json.obj()))

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
      send(Protocol.Out.tellRoomUser(simul.id, userId, makeMessage("redirect", pov.fullId)))
    }

  private val handler: Handler = {
    case Protocol.In.ChatSay(simulId, userId, msg) =>
      val publicSource = lila.hub.actorApi.shutup.PublicSource.Simul(simulId).some
      chat ! lila.chat.actorApi.UserTalk(Chat.Id(simulId), userId, msg, publicSource)
    case Protocol.In.ChatTimeout(simulId, modId, suspect, reason) =>
      chat ! lila.chat.actorApi.Timeout(Chat.Id(simulId), modId, suspect, reason, local = false)
    case P.In.DisconnectAll =>
      sockets.killAll
  }

  private val inReader: P.In.Reader = raw => Protocol.In.reader(raw) orElse P.In.baseReader(raw)

  remoteSocketApi.subscribe("simul-in", inReader)(handler orElse remoteSocketApi.baseHandler)

  private lazy val send: String => Unit = remoteSocketApi.makeSender("simul-out").apply _

  system.lilaBus.subscribeFun('remoteSocketChat) {
    case chatApi.ChatLine(chatId, line: UserLine) =>
      sockets.tell(chatId.value, NotifyVersion("message", lila.chat.JsonView(line), line.troll))
    case chatApi.OnTimeout(chatId, username) =>
      sockets.tell(chatId.value, NotifyVersion("chat_timeout", username, false))
    case chatApi.OnReinstate(chatId, userId) =>
      sockets.tell(chatId.value, NotifyVersion("chat_reinstate", userId, false))
    case a => println(s"remote socket chat unhandled $a")
  }
}

private object SimulSocket {

  case class NotifyVersion[A: Writes](tpe: String, data: A, troll: Boolean = false) {
    def msg = makeMessage(tpe, data)
  }

  object Protocol {
    object Out {
      def tellVersion(simulId: Simul.ID, payload: JsObject, version: SocketVersion, isTroll: Boolean) =
        s"tell/version $simulId $version $isTroll ${Json stringify payload}"
      def tellRoomUser(roomId: String, userId: User.ID, payload: JsObject) =
        s"tell/room/user $roomId $userId ${Json stringify payload}"
      def start(simulId: Simul.ID) =
        s"room/start $simulId"
      def stop(simulId: Simul.ID) =
        s"room/stop $simulId"
    }
    object In {
      case class ChatSay(simulId: Simul.ID, userId: User.ID, msg: String) extends P.In
      case class ChatTimeout(simulId: Simul.ID, userId: User.ID, suspect: String, reason: Reason) extends P.In

      val reader: P.In.Reader = raw => raw.path match {
        case "chat/say" => raw.args.split(" ", 3) match {
          case Array(simulId, userId, msg) => ChatSay(simulId, userId, msg).some
          case _ => none
        }
        case "chat/timeout" => raw.args.split(" ", 4) match {
          case Array(simulId, userId, suspect, reason) => Reason(reason) map {
            ChatTimeout(simulId, userId, suspect, _)
          }
          case _ => none
        }
        case _ => none
      }
    }
  }
}
