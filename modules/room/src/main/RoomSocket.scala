package lila.room

import play.api.libs.json.*
import scalalib.actor.SyncActorMap
import scalalib.actor.SyncActor

import lila.chat.{ BusChan, ChatApi, ChatTimeout, UserLine }
import lila.core.shutup.PublicSource
import lila.log.Logger
import lila.core.socket.{ protocol as P, * }
import lila.core.socket.{ makeMessage }
import lila.common.Json.given

import lila.user.{ Me, FlairApi }

object RoomSocket:

  type RoomsMap = SyncActorMap[RoomId, RoomState]

  case class NotifyVersion[A: Writes](tpe: String, data: A, troll: Boolean = false):
    def msg = makeMessage(tpe, data)

  final class RoomState(roomId: RoomId, send: SocketSend)(using Executor) extends SyncActor:

    private var version = SocketVersion(0)

    val process: SyncActor.Receive =
      case GetVersion(promise) => promise success version
      case SetVersion(v)       => version = v
      case nv: NotifyVersion[?] =>
        version = version.map(_ + 1)
        send:
          val tell =
            if chatMsgs(nv.tpe) then Protocol.Out.tellRoomChat
            else Protocol.Out.tellRoomVersion
          tell(roomId, nv.msg, version, nv.troll)

    override def stop() =
      super.stop()
      send(Protocol.Out.stop(roomId))

  def makeRoomMap(send: SocketSend)(using Executor) =
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
  )(using Executor): SocketHandler =
    ({
      case Protocol.In.ChatSay(roomId, userId, msg) =>
        chat.userChat
          .write(
            roomId.into(ChatId),
            userId,
            msg,
            publicSource(roomId)(PublicSource),
            chatBusChan
          )

      case Protocol.In.ChatTimeout(roomId, modId, suspect, reason, text) =>
        lila.chat.ChatTimeout.Reason(reason).foreach: r =>
          localTimeout.so { _(roomId, modId, suspect) } foreach: local =>
            val scope = if local then ChatTimeout.Scope.Local else ChatTimeout.Scope.Global
            chat.userChat.timeout(
              roomId.into(ChatId),
              suspect,
              r,
              text = text,
              scope = scope,
              busChan = chatBusChan
            )(using modId)
    }: SocketHandler).orElse(minRoomHandler(rooms, logger))

  def minRoomHandler(rooms: RoomsMap, logger: Logger): SocketHandler =
    case Protocol.In.KeepAlives(roomIds) => roomIds.foreach(rooms.touchOrMake)
    case P.In.WsBoot =>
      logger.warn("Remote socket boot")
    // rooms.killAll // apparently not
    case Protocol.In.SetVersions(versions) =>
      versions.foreach: (roomId, version) =>
        rooms.tell(RoomId(roomId), SetVersion(version))

  private val chatMsgs = Set("message", "chat_timeout", "chat_reinstate")

  def subscribeChat(rooms: RoomsMap, busChan: BusChan.Select)(using FlairApi.Getter)(using
      Executor
  ) =
    lila.common.Bus.subscribeFun(busChan(BusChan).chan, BusChan.Global.chan):
      case lila.chat.ChatLine(id, line: UserLine) =>
        lila.chat.JsonView(line) foreach: jsLine =>
          rooms.tellIfPresent(id.into(RoomId),(NotifyVersion)("message", jsLine, line.troll))
      case lila.chat.OnTimeout(id, userId) =>
        rooms.tellIfPresent(id.into(RoomId), NotifyVersion("chat_timeout", userId, troll = false))
      case lila.chat.OnReinstate(id, userId) =>
        rooms.tellIfPresent(id.into(RoomId), NotifyVersion("chat_reinstate", userId, troll = false))

  object Protocol:

    object In:

      case class ChatSay(roomId: RoomId, userId: UserId, msg: String) extends P.In
      case class ChatTimeout(roomId: RoomId, mod: Me.Id, suspect: UserId, reason: String, text: String)
          extends P.In
      case class KeepAlives(roomIds: Iterable[RoomId])                    extends P.In
      case class TellRoomSri(roomId: RoomId, tellSri: P.In.TellSri)       extends P.In
      case class SetVersions(versions: Iterable[(String, SocketVersion)]) extends P.In

      val reader: P.In.Reader =
        case P.RawMsg("room/alives", raw) => KeepAlives(P.In.commas(raw.args) map { RoomId(_) }).some
        case P.RawMsg("chat/say", raw) =>
          raw.get(3) { case Array(roomId, userId, msg) =>
            ChatSay(RoomId(roomId), UserId(userId), msg).some
          }
        case P.RawMsg("chat/timeout", raw) =>
          raw.get(5) { case Array(roomId, userId, suspect, reason, text) =>
            ChatTimeout(RoomId(roomId), Me.Id(userId), UserId(suspect), reason, text).some
          }
        case P.RawMsg("tell/room/sri", raw) =>
          raw.get(4) { case arr @ Array(roomId, _, _, _) =>
            P.In.tellSriMapper.lift(arr drop 1).flatten map {
              TellRoomSri(RoomId(roomId), _)
            }
          }
        case P.RawMsg("room/versions", raw) =>
          SetVersions(P.In.commas(raw.args) map {
            _.split(':') match
              case Array(roomId, v) => (roomId, SocketVersion(java.lang.Integer.parseInt(v)))
          }).some

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
