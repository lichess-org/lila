package lila.room

import lila.chat.{ BusChan, ChatApi, ChatTimeout, UserLine }
import lila.hub.actorApi.shutup.PublicSource
import lila.hub.{ SyncActor, SyncActorMap }
import lila.log.Logger
import lila.socket.{ SocketVersion, GetVersion }
import lila.socket.RemoteSocket.{ Protocol as P, * }
import lila.socket.Socket.{ makeMessage }
import lila.common.Json.given

import play.api.libs.json.*
import lila.user.Me

object RoomSocket:

  type RoomsMap = SyncActorMap[RoomId, RoomState]

  case class NotifyVersion[A: Writes](tpe: String, data: A, troll: Boolean = false):
    def msg = makeMessage(tpe, data)

  final class RoomState(roomId: RoomId, send: Send)(using Executor) extends SyncActor:

    private var version = SocketVersion(0)

    val process: SyncActor.Receive =
      case GetVersion(promise) => promise success version
      case SetVersion(v)       => version = v
      case nv: NotifyVersion[?] =>
        version = version.incVersion
        send:
          val tell =
            if chatMsgs(nv.tpe) then Protocol.Out.tellRoomChat
            else Protocol.Out.tellRoomVersion
          tell(roomId, nv.msg, version, nv.troll)

    override def stop() =
      super.stop()
      send(Protocol.Out.stop(roomId))

  def makeRoomMap(send: Send)(using Executor) =
    SyncActorMap[RoomId, RoomState](
      mkActor = roomId => RoomState(roomId, send),
      accessTimeout = 5 minutes
    )

  def roomHandler(
      rooms: RoomsMap,
      chat: ChatApi,
      logger: Logger,
      publicSource: RoomId => PublicSource.type => Option[PublicSource],
      localTimeout: Option[(RoomId, UserId, UserId) => Fu[Boolean]] = None,
      chatBusChan: BusChan.Select
  )(using Executor): Handler =
    ({
      case Protocol.In.ChatSay(roomId, userId, msg) =>
        chat.userChat
          .write(
            roomId into ChatId,
            userId,
            msg,
            publicSource(roomId)(PublicSource),
            chatBusChan
          )

      case Protocol.In.ChatTimeout(roomId, modId, suspect, reason, text) =>
        lila.chat.ChatTimeout.Reason(reason) foreach { r =>
          localTimeout.so { _(roomId, modId, suspect) } foreach { local =>
            val scope = if local then ChatTimeout.Scope.Local else ChatTimeout.Scope.Global
            chat.userChat.timeout(
              roomId into ChatId,
              suspect,
              r,
              text = text,
              scope = scope,
              busChan = chatBusChan
            )(using modId)
          }
        }
    }: Handler) orElse minRoomHandler(rooms, logger)

  def minRoomHandler(rooms: RoomsMap, logger: Logger): Handler =
    case Protocol.In.KeepAlives(roomIds) => roomIds foreach rooms.touchOrMake
    case P.In.WsBoot =>
      logger.warn("Remote socket boot")
    // rooms.killAll // apparently not
    case Protocol.In.SetVersions(versions) =>
      versions.foreach: (roomId, version) =>
        rooms.tell(RoomId(roomId), SetVersion(version))

  private val chatMsgs = Set("message", "chat_timeout", "chat_reinstate")

  def subscribeChat(rooms: RoomsMap, busChan: BusChan.Select) =
    lila.common.Bus.subscribeFun(busChan(BusChan).chan, BusChan.Global.chan):
      case lila.chat.ChatLine(id, line: UserLine) =>
        rooms.tellIfPresent(id into RoomId, NotifyVersion("message", lila.chat.JsonView(line), line.troll))
      case lila.chat.OnTimeout(id, userId) =>
        rooms.tellIfPresent(id into RoomId, NotifyVersion("chat_timeout", userId, troll = false))
      case lila.chat.OnReinstate(id, userId) =>
        rooms.tellIfPresent(id into RoomId, NotifyVersion("chat_reinstate", userId, troll = false))

  object Protocol:

    object In:

      case class ChatSay(roomId: RoomId, userId: UserId, msg: String) extends P.In
      case class ChatTimeout(roomId: RoomId, mod: Me.Id, suspect: UserId, reason: String, text: String)
          extends P.In
      case class KeepAlives(roomIds: Iterable[RoomId])                    extends P.In
      case class TellRoomSri(roomId: RoomId, tellSri: P.In.TellSri)       extends P.In
      case class SetVersions(versions: Iterable[(String, SocketVersion)]) extends P.In

      val reader: P.In.Reader = raw =>
        raw.path match
          case "room/alives" => KeepAlives(P.In.commas(raw.args) map { RoomId(_) }).some
          case "chat/say" =>
            raw.get(3) { case Array(roomId, userId, msg) =>
              ChatSay(RoomId(roomId), UserId(userId), msg).some
            }
          case "chat/timeout" =>
            raw.get(5) { case Array(roomId, userId, suspect, reason, text) =>
              ChatTimeout(RoomId(roomId), Me.Id(userId), UserId(suspect), reason, text).some
            }
          case "tell/room/sri" =>
            raw.get(4) { case arr @ Array(roomId, _, _, _) =>
              P.In.tellSriMapper.lift(arr drop 1).flatten map {
                TellRoomSri(RoomId(roomId), _)
              }
            }
          case "room/versions" =>
            SetVersions(P.In.commas(raw.args) map {
              _.split(':') match
                case Array(roomId, v) => (roomId, SocketVersion(java.lang.Integer.parseInt(v)))
            }).some
          case _ => P.In.baseReader(raw)

    object Out:

      def tellRoom(roomId: RoomId, payload: JsObject) =
        s"tell/room $roomId ${Json stringify payload}"
      def tellRoomVersion(roomId: RoomId, payload: JsObject, version: SocketVersion, isTroll: Boolean) =
        s"tell/room/version $roomId $version ${P.Out.boolean(isTroll)} ${Json stringify payload}"
      def tellRoomUser(roomId: RoomId, userId: UserId, payload: JsObject) =
        s"tell/room/user $roomId $userId ${Json stringify payload}"
      def tellRoomUsers(roomId: RoomId, userIds: Iterable[UserId], payload: JsObject) =
        s"tell/room/users $roomId ${P.Out.commas(userIds)} ${Json stringify payload}"
      def tellRoomChat(roomId: RoomId, payload: JsObject, version: SocketVersion, isTroll: Boolean) =
        s"tell/room/chat $roomId $version ${P.Out.boolean(isTroll)} ${Json stringify payload}"
      def stop(roomId: RoomId) =
        s"room/stop $roomId"

  case class SetVersion(version: SocketVersion)
