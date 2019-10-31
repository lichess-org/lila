package lila.round

import play.api.libs.json._
import scala.concurrent.duration._

import actorApi._
import actorApi.round._
import chess.{ Color, White, Black, Speed }
import lila.chat.Chat
import lila.common.Bus
import lila.game.{ Game, Event }
import lila.hub.{ Trouper, TrouperMap, DuctMap }
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

  def tellRoom(game: Game, msg: Any): Unit = tellRoom(GameId(game.id), msg)
  def tellRoom(gameId: GameId, msg: Any): Unit = rooms.tell(gameId.value, msg)

  def tellRound(gameId: GameId, msg: Any): Unit = rounds.tell(gameId.value, msg)

  private lazy val roundHandler: Handler = {
    case RP.In.ChatSay(roomId, userId, msg) => messenger.watcher(roomId.value, userId, msg)
    case RP.In.TellRoomSri(gameId, P.In.TellSri(sri, user, tpe, o)) => tpe match {
      case t => logger.warn(s"Unhandled round socket message: $t")
    }
    case Protocol.In.PlayerDo(fullId, tpe, o) => tpe match {
      case "moretime" => tellRound(fullId.gameId, Moretime(fullId.playerId.value))
      case t => logger.warn(s"Unhandled round socket message: $t")
    }
    case ping: Protocol.In.PlayerPing => tellRoom(ping.gameId, ping)
    case RP.In.KeepAlives(roomIds) => roomIds foreach { roomId =>
      rooms touchOrMake roomId.value
    }
  }

  private lazy val send: String => Unit = remoteSocketApi.makeSender("round-out").apply _

  remoteSocketApi.subscribe("round-in", Protocol.In.reader)(
    roundHandler orElse remoteSocketApi.baseHandler
  )

  private var rounds: DuctMap[RoundDuct] = null
  private[round] def setRoundMap(r: DuctMap[RoundDuct]): Unit = {
    rounds = r
  }
}

object RoundRemoteSocket {

  case class GameId(value: String) extends AnyVal with StringValue {
    def full(playerId: PlayerId) = FullId(s"$value{$playerId.value}")
  }
  case class FullId(value: String) extends AnyVal with StringValue {
    def gameId = GameId(value take Game.gameIdSize)
    def playerId = PlayerId(value drop Game.gameIdSize)
  }
  case class PlayerId(value: String) extends AnyVal with StringValue

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
      case Protocol.In.PlayerPing(_, color) => playerDo(color, _.ping)
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

    object In {

      case class PlayerPing(gameId: GameId, color: Color) extends P.In
      case class PlayerDo(fullId: FullId, tpe: String, msg: JsObject) extends P.In

      val reader: P.In.Reader = raw => raw.path match {
        case "round/w" => PlayerPing(GameId(raw.args), chess.White).some
        case "round/b" => PlayerPing(GameId(raw.args), chess.Black).some
        case "round/do" => raw.get(2) {
          case Array(fullId, payload) => for {
            obj <- Json.parse(payload).asOpt[JsObject]
            tpe <- obj str "t"
          } yield PlayerDo(FullId(fullId), tpe, obj)
        }
        case _ => RP.In.reader(raw)
      }
    }
  }
}
