package lila.room

import lila.chat.{ Chat, UserLine, actorApi => chatApi }
import lila.common.Bus
import lila.hub.actorApi.shutup.PublicSource
import lila.hub.{ Trouper, TrouperMap }
import lila.log.Logger
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.{ makeMessage, GetVersion, SocketVersion, Sri }
import lila.user.User

import play.api.libs.json._
import scala.concurrent.duration._

object RoomSocket {

  case class RoomId(value: String) extends AnyVal with StringValue

  case class NotifyVersion[A: Writes](tpe: String, data: A, troll: Boolean = false) {
    def msg = makeMessage(tpe, data)
  }

  final class RoomState(roomId: RoomId, send: Send, bus: Bus) extends Trouper {

    private val chatId = Chat.Id(roomId.value)
    private def chatClassifier = Chat classify chatId
    private var version = SocketVersion(0)

    val process: Trouper.Receive = {
      case GetVersion(promise) => promise success version
      case SetVersion(v) => version = v
      case nv: NotifyVersion[_] =>
        version = version.inc
        send(Protocol.Out.tellRoomVersion(roomId, nv.msg, version, nv.troll))
      case lila.chat.actorApi.ChatLine(_, line) => line match {
        case line: UserLine => this ! NotifyVersion("message", lila.chat.JsonView(line), line.troll)
        case _ =>
      }
      case chatApi.OnTimeout(userId) =>
        this ! NotifyVersion("chat_timeout", userId, false)
      case chatApi.OnReinstate(userId) =>
        this ! NotifyVersion("chat_reinstate", userId, false)
    }
    override def stop() {
      super.stop()
      send(Protocol.Out.stop(roomId))
      bus.unsubscribe(this, chatClassifier)
    }
    bus.subscribe(this, chatClassifier)
  }

  def makeRoomMap(send: Send, bus: Bus) = new TrouperMap(
    mkTrouper = roomId => new RoomState(RoomId(roomId), send, bus),
    accessTimeout = 5 minutes
  )

  def roomHandler(
    rooms: TrouperMap[RoomState],
    chat: akka.actor.ActorSelection,
    logger: Logger,
    publicSource: RoomId => PublicSource.type => Option[PublicSource],
    localTimeout: Option[(RoomId, User.ID, User.ID) => Fu[Boolean]] = None
  ): Handler = ({
    case Protocol.In.ChatSay(roomId, userId, msg) =>
      chat ! lila.chat.actorApi.UserTalk(Chat.Id(roomId.value), userId, msg, publicSource(roomId)(PublicSource))
    case Protocol.In.ChatTimeout(roomId, modId, suspect, reason) => lila.chat.ChatTimeout.Reason(reason) foreach { r =>
      localTimeout.?? { _(roomId, modId, suspect) } foreach { local =>
        chat ! lila.chat.actorApi.Timeout(Chat.Id(roomId.value), modId, suspect, r, local = local)
      }
    }
  }: Handler) orElse minRoomHandler(rooms, logger)

  def minRoomHandler(rooms: TrouperMap[RoomState], logger: Logger): Handler = {
    case Protocol.In.KeepAlives(roomIds) => roomIds foreach { roomId =>
      rooms touchOrMake roomId.value
    }
    case P.In.WsBoot =>
      logger.warn("Remote socket boot")
      rooms.killAll
    case Protocol.In.SetVersions(versions) => versions foreach {
      case (roomId, version) => rooms.tell(roomId, SetVersion(version))
    }
  }

  object Protocol {

    object In {

      case class ChatSay(roomId: RoomId, userId: String, msg: String) extends P.In
      case class ChatTimeout(roomId: RoomId, userId: String, suspect: String, reason: String) extends P.In
      case class KeepAlives(roomIds: Iterable[RoomId]) extends P.In
      case class TellRoomSri(roomId: RoomId, tellSri: P.In.TellSri) extends P.In
      case class SetVersions(versions: Iterable[(String, SocketVersion)]) extends P.In

      val reader: P.In.Reader = raw => raw.path match {
        case "room/alives" => KeepAlives(raw.args split "," map RoomId.apply).some
        case "chat/say" => raw.get(3) {
          case Array(roomId, userId, msg) => ChatSay(RoomId(roomId), userId, msg).some
        }
        case "chat/timeout" => raw.get(4) {
          case Array(roomId, userId, suspect, reason) => ChatTimeout(RoomId(roomId), userId, suspect, reason).some
        }
        case "tell/room/sri" => raw.get(4) {
          case arr @ Array(roomId, _, _, _) => P.In.tellSriMapper.lift(arr drop 1).flatten map {
            TellRoomSri(RoomId(roomId), _)
          }
        }
        case "room/versions" => SetVersions(P.In.commas(raw.args) map {
          _.split(':') match {
            case Array(roomId, v) => (roomId, SocketVersion(java.lang.Integer.parseInt(v)))
          }
        }).some
        case _ => P.In.baseReader(raw)
      }
    }

    object Out {

      def tellRoom(roomId: RoomId, payload: JsObject) =
        s"tell/room $roomId ${Json stringify payload}"
      def tellRoomVersion(roomId: RoomId, payload: JsObject, version: SocketVersion, isTroll: Boolean) =
        s"tell/room/version $roomId $version ${P.Out.boolean(isTroll)} ${Json stringify payload}"
      def tellRoomUser(roomId: RoomId, userId: User.ID, payload: JsObject) =
        s"tell/room/user $roomId $userId ${Json stringify payload}"
      def tellRoomUsers(roomId: RoomId, userIds: Iterable[User.ID], payload: JsObject) =
        s"tell/room/users $roomId ${P.Out.commas(userIds)} ${Json stringify payload}"
      def stop(roomId: RoomId) =
        s"room/stop $roomId"
    }
  }

  case class SetVersion(version: SocketVersion)
}
