package lila.round

import scala.concurrent.duration._

import actorApi._
import chess.{ Color, White, Black, Speed }
import lila.chat.Chat
import lila.common.Bus
import lila.game.{ Game, Event }
import lila.hub.{ Trouper, TrouperMap }
import lila.room.RoomSocket.{ Protocol => RP, _ }
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.{ Sri, SocketVersion, GetVersion, makeMessage }
import lila.user.User

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
    case Protocol.In.RoundPlayerPing(gameId, color) =>
      rooms.tell(gameId, Protocol.PlayerPing(color))
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

    private var mightBeSimul = true // until proven false
    private var gameSpeed: Option[Speed] = none

    private final class Player(color: Color) {

      // when the player has been seen online for the last time
      private var time: Long = nowMillis
      // wether the player closed the window intentionally
      private var bye: Int = 0
      // connected as a bot
      private var botConnected: Boolean = false

      var userId = none[User.ID]
      var goneWeight = 1f

      def ping: Unit = {
        // TODO isGone foreach { _ ?? notifyGone(color, false) }
        if (bye > 0) bye = bye - 1
        time = nowMillis
      }
      def setBye: Unit = {
        bye = 3
      }
      private def isBye = bye > 0

      // TODO private def isHostingSimul: Fu[Boolean] = mightBeSimul ?? userId ?? { u =>
      //   lilaBus.ask[Set[User.ID]]('simulGetHosts)(GetHostIds).map(_ contains u)
      // }

      private def timeoutMillis = {
        if (isBye) RoundSocket.ragequitTimeout.toMillis else RoundSocket.gameDisconnectTimeout(gameSpeed).toMillis
      } * goneWeight atLeast 12000

      def isConnected: Boolean =
        time >= (nowMillis - timeoutMillis) || botConnected

      def isGone: Fu[Boolean] = fuccess {
        !isConnected
      } // TODO ?? !isHostingSimul

      def setBotConnected(v: Boolean) =
        botConnected = v

      def isBotConnected = botConnected
    }

    private val whitePlayer = new Player(White)
    private val blackPlayer = new Player(Black)

    private def notifyVersion(nv: NotifyVersion[_]): Unit = {
      version = version.inc
      send(RP.Out.tellRoomVersion(roomId, nv.msg, version, nv.troll))
    }

    val process: Trouper.Receive = {
      case Protocol.PlayerPing(color) => playerDo(color, _.ping)
      case GetVersion(promise) => promise success version
      case nv: NotifyVersion[_] =>
        version = version.inc
        send(RP.Out.tellRoomVersion(roomId, nv.msg, version, nv.troll))
      case EventList(events) => events map { e =>
        version = version.inc
        send(RP.Out.tellRoomVersion(roomId, makeMessage(e.typ, e.data), version, e.troll))
      }
      case GetSocketStatus(promise) =>
        playerGet(White, _.isGone) zip playerGet(Black, _.isGone) foreach {
          case (whiteIsGone, blackIsGone) => promise success SocketStatus(
            version = version,
            whiteOnGame = ownerIsHere(White),
            whiteIsGone = whiteIsGone,
            blackOnGame = ownerIsHere(Black),
            blackIsGone = blackIsGone
          )
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

    def ownerIsHere(color: Color) = playerGet(color, _.isConnected)

    def playerGet[A](color: Color, getter: Player => A): A =
      getter(color.fold(whitePlayer, blackPlayer))

    def playerDo(color: Color, effect: Player => Unit): Unit =
      effect(color.fold(whitePlayer, blackPlayer))

    override def stop() {
      super.stop()
      send(RP.Out.stop(roomId))
      bus.unsubscribe(this, chatClassifier)
    }
    send(RP.Out.start(roomId))
    bus.subscribe(this, chatClassifier)
  }

  object Protocol {

    case class PlayerPing(color: Color)

    object In {

      case class TellRoundSri(gameId: Game.ID, tellSri: P.In.TellSri) extends P.In
      case class RoundPlayerPing(gameId: Game.ID, color: Color) extends P.In

      val reader: P.In.Reader = raw => roundReader(raw) orElse RP.In.reader(raw)

      val roundReader: P.In.Reader = raw => raw.path match {
        case "tell/round/sri" => raw.get(4) {
          case arr @ Array(gameId, _, _, _) => P.In.tellSriMapper.lift(arr drop 1).flatten map {
            TellRoundSri(gameId, _)
          }
        }
        case "round/w" => RoundPlayerPing(raw.args, chess.White).some
        case "round/b" => RoundPlayerPing(raw.args, chess.Black).some
        case _ => none
      }
    }
  }
}
