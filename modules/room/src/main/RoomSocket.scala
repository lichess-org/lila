package lila.room

import lila.chat.{ BusChan, Chat, ChatApi, ChatTimeout, UserLine }
import lila.hub.actorApi.shutup.PublicSource
import lila.hub.{ Trouper, TrouperMap }
import lila.log.Logger
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.{ makeMessage, GetVersion, SocketVersion }
import lila.user.User

import play.api.libs.json._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

object RoomSocket {

  case class RoomId(value: String) extends AnyVal with StringValue

  case class NotifyVersion[A: Writes](tpe: String, data: A, troll: Boolean = false) {
    def msg = makeMessage(tpe, data)
  }

  final class RoomState(roomId: RoomId, send: Send)(implicit
      ec: ExecutionContext
  ) extends Trouper {

    private var version = SocketVersion(0)

    val process: Trouper.Receive = {
      case GetVersion(promise) => promise success version
      case SetVersion(v)       => version = v
      case nv: NotifyVersion[_] =>
        version = version.inc
        send {
          val tell =
            if (chatMsgs(nv.tpe)) Protocol.Out.tellRoomChat _
            else Protocol.Out.tellRoomVersion _
          tell(roomId, nv.msg, version, nv.troll)
        }
    }
    override def stop() = {
      super.stop()
      send(Protocol.Out.stop(roomId))
    }
  }

  def makeRoomMap(send: Send)(implicit
      ec: ExecutionContext,
      mode: play.api.Mode
  ) =
    new TrouperMap(
      mkTrouper = roomId =>
        new RoomState(
          RoomId(roomId),
          send
        ),
      accessTimeout = 5 minutes
    )

  def roomHandler(
      rooms: TrouperMap[RoomState],
      chat: ChatApi,
      logger: Logger,
      publicSource: RoomId => PublicSource.type => Option[PublicSource],
      localTimeout: Option[(RoomId, User.ID, User.ID) => Fu[Boolean]] = None,
      chatBusChan: BusChan.Select
  )(implicit ec: ExecutionContext): Handler =
    ({
      case Protocol.In.ChatSay(roomId, userId, msg) =>
        chat.userChat
          .write(
            Chat.Id(roomId.value),
            userId,
            msg,
            publicSource(roomId)(PublicSource),
            chatBusChan
          )
          .unit
      case Protocol.In.ChatTimeout(roomId, modId, suspect, reason, text) =>
        lila.chat.ChatTimeout.Reason(reason) foreach { r =>
          localTimeout.?? { _(roomId, modId, suspect) } foreach { local =>
            val scope = if (local) ChatTimeout.Scope.Local else ChatTimeout.Scope.Global
            chat.userChat.timeout(
              Chat.Id(roomId.value),
              modId,
              suspect,
              r,
              text = text,
              scope = scope,
              busChan = chatBusChan
            )
          }
        }
    }: Handler) orElse minRoomHandler(rooms, logger)

  def minRoomHandler(rooms: TrouperMap[RoomState], logger: Logger): Handler = {
    case Protocol.In.KeepAlives(roomIds) =>
      roomIds foreach { roomId =>
        rooms touchOrMake roomId.value
      }
    case P.In.WsBoot =>
      logger.warn("Remote socket boot")
    // rooms.killAll // apparently not
    case Protocol.In.SetVersions(versions) =>
      versions foreach { case (roomId, version) =>
        rooms.tell(roomId, SetVersion(version))
      }
  }

  private val chatMsgs = Set("message", "chat_timeout", "chat_reinstate")

  def subscribeChat(rooms: TrouperMap[RoomState], busChan: BusChan.Select) = {
    import lila.chat.actorApi._
    lila.common.Bus.subscribeFun(busChan(BusChan).chan, BusChan.Global.chan) {
      case ChatLine(id, line: UserLine) =>
        rooms.tellIfPresent(id.value, NotifyVersion("message", lila.chat.JsonView(line), line.troll))
      case OnTimeout(id, userId) =>
        rooms.tellIfPresent(id.value, NotifyVersion("chat_timeout", userId, troll = false))
      case OnReinstate(id, userId) =>
        rooms.tellIfPresent(id.value, NotifyVersion("chat_reinstate", userId, troll = false))
    }
  }

  object Protocol {

    object In {

      case class ChatSay(roomId: RoomId, userId: String, msg: String) extends P.In
      case class ChatTimeout(roomId: RoomId, userId: String, suspect: String, reason: String, text: String)
          extends P.In
      case class KeepAlives(roomIds: Iterable[RoomId])                    extends P.In
      case class TellRoomSri(roomId: RoomId, tellSri: P.In.TellSri)       extends P.In
      case class SetVersions(versions: Iterable[(String, SocketVersion)]) extends P.In

      val reader: P.In.Reader = raw =>
        raw.path match {
          case "room/alives" => KeepAlives(P.In.commas(raw.args) map RoomId.apply).some
          case "chat/say" =>
            raw.get(3) { case Array(roomId, userId, msg) =>
              ChatSay(RoomId(roomId), userId, msg).some
            }
          case "chat/timeout" =>
            raw.get(5) { case Array(roomId, userId, suspect, reason, text) =>
              ChatTimeout(RoomId(roomId), userId, suspect, reason, text).some
            }
          case "tell/room/sri" =>
            raw.get(4) { case arr @ Array(roomId, _, _, _) =>
              P.In.tellSriMapper.lift(arr drop 1).flatten map {
                TellRoomSri(RoomId(roomId), _)
              }
            }
          case "room/versions" =>
            SetVersions(P.In.commas(raw.args) map {
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
      def tellRoomChat(roomId: RoomId, payload: JsObject, version: SocketVersion, isTroll: Boolean) =
        s"tell/room/chat $roomId $version ${P.Out.boolean(isTroll)} ${Json stringify payload}"
      def stop(roomId: RoomId) =
        s"room/stop $roomId"
    }
  }

  case class SetVersion(version: SocketVersion)
}
