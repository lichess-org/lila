package lila.socket

import akka.actor.{ CoordinatedShutdown, Scheduler }
import chess.{ Centis, Color }
import io.lettuce.core.*
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection as PubSub
import play.api.libs.json.*

import java.util.concurrent.atomic.AtomicReference

import lila.common.{ Bus, Lilakka }
import lila.core.misc.streamer.StreamersOnline
import lila.core.relation.{ Follow, UnFollow }
import lila.core.round.Mlat
import lila.core.security.CloseAccount
import lila.core.socket.remote.*
import lila.core.socket.{ SocketRequester as _, * }

final class RemoteSocket(
    redisClient: RedisClient,
    shutdown: CoordinatedShutdown,
    requester: SocketRequester,
    userLag: UserLagCache
)(using Executor, Scheduler):

  import RemoteSocket.*, Protocol.*

  private var stopping = false

  private type UserIds = Set[UserId]

  val onlineUserIds: AtomicReference[Set[UserId]] = AtomicReference(initialUserIds)

  def kit         = SocketKit(subscribe, channel => makeSender(channel, 1).send, baseHandler)
  def parallelKit = ParallelSocketKit(subscribeRoundRobin, makeSender, baseHandler)

  val baseHandler: SocketHandler =
    case In.ConnectUser(userId) =>
      onlineUserIds.getAndUpdate(_ + userId)
    case In.ConnectUsers(userIds) =>
      onlineUserIds.getAndUpdate(_ ++ userIds)
    case In.DisconnectUsers(userIds) =>
      onlineUserIds.getAndUpdate(_ -- userIds)
    case In.NotifiedBatch(userIds) =>
      Bus.publish(lila.core.notify.NotifiedBatch(userIds), "notify")
    case In.Lags(lags) =>
      lags.foreach: (userId, centis) =>
        userLag.put(userId, centis)
      // this shouldn't be necessary... ensure that users are known to be online
      onlineUserIds.getAndUpdate((x: UserIds) => x ++ lags.keys)
    case In.TellSri(sri, userId, typ, msg) =>
      Bus.publish(TellSriIn(sri.value, userId, msg), s"remoteSocketIn:$typ")
    case In.TellUser(userId, typ, msg) =>
      Bus.publish(TellUserIn(userId, msg), s"remoteSocketIn:$typ")
    case In.ReqResponse(reqId, response) => requester.onResponse(reqId, response)
    case In.Ping(id)                     => send.exec(Out.pong(id))
    case In.WsBoot =>
      logger.warn("Remote socket boot")
      onlineUserIds.set(initialUserIds)

  Bus.subscribeFun(
    "socketUsers",
    "announce",
    "mlat",
    "sendToFlag",
    "remoteSocketOut",
    "accountClose",
    "shadowban",
    "impersonate",
    "relation",
    "onlineApiUsers"
  ):
    case SendTos(userIds, payload) =>
      val connectedUsers = userIds.intersect(onlineUserIds.get)
      if connectedUsers.nonEmpty then send.exec(Out.tellUsers(connectedUsers, payload))
    case SendTo(userId, payload) =>
      if onlineUserIds.get.contains(userId) then send.exec(Out.tellUser(userId, payload))
    case SendToOnlineUser(userId, makePayload) =>
      if onlineUserIds.get.contains(userId) then
        makePayload.value.foreach: payload =>
          send.exec(Out.tellUser(userId, payload))
    case Announce(_, _, json) =>
      send.exec(Out.tellAll(Json.obj("t" -> "announce", "d" -> json)))
    case Mlat(millis) =>
      send.exec(Out.mlat(millis))
    case SendToFlag(flag, payload) =>
      send.exec(Out.tellFlag(flag, payload))
    case TellSriOut(sri, payload) =>
      send.exec(Out.tellSri(Sri(sri), payload))
    case TellSrisOut(sris, payload) =>
      send.exec(Out.tellSris(Sri.from(sris), payload))
    case CloseAccount(userId) =>
      send.exec(Out.disconnectUser(userId))
    case lila.core.mod.Shadowban(userId, v) =>
      send.exec(Out.setTroll(userId, v))
    case lila.core.mod.Impersonate(userId, modId) =>
      send.exec(Out.impersonate(userId, modId))
    case ApiUserIsOnline(userId, value) =>
      send.exec(Out.apiUserOnline(userId, value))
      if value then onlineUserIds.getAndUpdate(_ + userId)
    case Follow(u1, u2)   => send.exec(Out.follow(u1, u2))
    case UnFollow(u1, u2) => send.exec(Out.unfollow(u1, u2))
  Bus.sub[StreamersOnline]:
    case StreamersOnline(streamers) =>
      send.exec(Out.streamersOnline(streamers))

  final class StoppableSender(val conn: PubSub[String, String], channel: Channel) extends Sender:
    def apply(msg: String)               = if !stopping then super.sendTo(channel, msg)
    def sticky(_id: String, msg: String) = apply(msg)

  final class RoundRobinSender(val conn: PubSub[String, String], channel: Channel, parallelism: Int)
      extends Sender:
    def apply(msg: String): Unit = publish(msg.hashCode.abs % parallelism, msg)
    // use the ID to select the channel, not the entire message
    def sticky(id: String, msg: String): Unit = publish(id.hashCode.abs % parallelism, msg)

    private def publish(subChannel: Int, msg: String) =
      if !stopping then conn.async.publish(s"$channel:$subChannel", msg)

  def makeSender(channel: Channel, parallelism: Int = 1): Sender =
    if parallelism > 1 then RoundRobinSender(redisClient.connectPubSub(), channel, parallelism)
    else StoppableSender(redisClient.connectPubSub(), channel)

  private val send: SocketSend = makeSender("site-out").send

  def subscribe(channel: Channel, reader: In.Reader)(handler: SocketHandler): Funit =
    val fullReader = reader.orElse(Protocol.In.baseReader)
    connectAndSubscribe(channel): str =>
      val parts = str.split(" ", 2)
      parts.headOption
        .map:
          new lila.core.socket.protocol.RawMsg(_, ~parts.lift(1))
        .match
          case None => logger.error(s"Invalid $channel $str")
          case Some(raw) =>
            fullReader
              .applyOrElse(
                raw,
                raw =>
                  logger.info(s"Unread $channel $raw")
                  none
              )
              .collect(handler) match
              case Some(_) => // processed
              case None    => logger.info(s"Unhandled $channel $str")

  def subscribeRoundRobin(channel: Channel, reader: In.Reader, parallelism: Int)(
      handler: SocketHandler
  ): Funit =
    // subscribe to main channel
    subscribe(channel, reader)(handler) >> {
      // and subscribe to subchannels
      (0 to parallelism)
        .parallelVoid(index => subscribe(s"$channel:$index", reader)(handler))
    }

  private def connectAndSubscribe(channel: Channel)(f: String => Unit): Funit =
    val conn = redisClient.connectPubSub()
    conn.addListener(
      new pubsub.RedisPubSubAdapter[String, String]:
        override def message(_channel: String, message: String): Unit = f(message)
    )
    val subPromise = Promise[Unit]()
    conn.async
      .subscribe(channel)
      .thenRun: () =>
        subPromise.success(())
    subPromise.future

  Lilakka.shutdown(shutdown, _.PhaseBeforeServiceUnbind, "Telling lila-ws we're stopping"): () =>
    requester[Unit](
      id => send.exec(Protocol.Out.stop(id)),
      res => logger.info(s"lila-ws says: $res")
    ).withTimeout(1.second, "Lilakka.shutdown")
      .addFailureEffect(e => logger.error("lila-ws stop", e))
      .recoverDefault

  Lilakka.shutdown(shutdown, _.PhaseServiceUnbind, "Stopping the socket redis pool"): () =>
    Future:
      stopping = true
      redisClient.shutdown()

object RemoteSocket:

  trait Sender extends ParallelSocketSend:
    protected val conn: PubSub[String, String]
    protected def sendTo(channel: Channel, msg: String) = conn.async.publish(channel, msg)

  object Protocol:

    trait In
    object In:

      export lila.core.socket.protocol.In.*
      import lila.core.socket.protocol.RawMsg

      val baseReader: Reader =
        case RawMsg("connect/user", raw)     => ConnectUser(UserId(raw.args)).some
        case RawMsg("connect/users", raw)    => ConnectUsers(UserId.from(commas(raw.args))).some
        case RawMsg("disconnect/users", raw) => DisconnectUsers(UserId.from(commas(raw.args))).some
        case RawMsg("connect/sris", raw) =>
          ConnectSris {
            commas(raw.args).map(_.split(' ')).map { s =>
              (Sri(s(0)), UserId.from(s.lift(1)))
            }
          }.some
        case RawMsg("disconnect/sris", raw) => DisconnectSris(commas(raw.args).map { Sri(_) }).some
        case RawMsg("notified/batch", raw)  => NotifiedBatch(UserId.from(commas(raw.args))).some
        case RawMsg("lag", raw) =>
          raw.all.pipe { s =>
            Centis.from(s.lift(1).flatMap(_.toIntOption)).map { Lag(UserId(s(0)), _) }
          }
        case RawMsg("lags", raw) =>
          Lags(commas(raw.args).flatMap {
            _.split(':') match
              case Array(user, l) =>
                l.toIntOption.map { lag =>
                  UserId(user) -> Centis(lag)
                }
              case _ => None
          }.toMap).some
        case RawMsg("tell/sri", raw) => raw.get(3)(lila.core.socket.protocol.In.tellSriMapper)
        case RawMsg("tell/user", raw) =>
          raw.get(2) { case Array(user, payload) =>
            for
              obj <- Json.parse(payload).asOpt[JsObject]
              typ <- obj.str("t")
            yield TellUser(UserId(user), typ, obj)
          }
        case RawMsg("req/response", raw) =>
          raw.get(2) { case Array(reqId, response) =>
            reqId.toIntOption.map { ReqResponse(_, response) }
          }
        case RawMsg("ping", raw) => Ping(raw.args).some
        case RawMsg("boot", raw) => WsBoot.some

    object Out:
      export lila.core.socket.protocol.Out.*
      def tellUser(userId: UserId, payload: JsObject) =
        s"tell/users $userId ${Json.stringify(payload)}"
      def tellUsers(userIds: Set[UserId], payload: JsObject) =
        s"tell/users ${commas(userIds)} ${Json.stringify(payload)}"
      def tellAll(payload: JsObject) =
        s"tell/all ${Json.stringify(payload)}"
      def tellFlag(flag: String, payload: JsObject) =
        s"tell/flag $flag ${Json.stringify(payload)}"
      def mlat(millis: Int) =
        s"mlat ${millis}"
      def disconnectUser(userId: UserId) =
        s"disconnect/user $userId"
      def setTroll(userId: UserId, v: Boolean) =
        s"mod/troll/set $userId ${boolean(v)}"
      def impersonate(userId: UserId, by: Option[UserId]) =
        s"mod/impersonate $userId ${optional(by.map(_.value))}"
      def follow(u1: UserId, u2: UserId)       = s"rel/follow $u1 $u2"
      def unfollow(u1: UserId, u2: UserId)     = s"rel/unfollow $u1 $u2"
      def apiUserOnline(u: UserId, v: Boolean) = s"api/online $u ${boolean(v)}"
      def streamersOnline(streamers: Iterable[(UserId, String)]) =
        s"streamers/online ${commas(streamers.map { (u, s) => s"$u:$s" })}"
      def respond(reqId: Int, payload: JsObject) = s"req/response $reqId ${Json.stringify(payload)}"
      def stop(reqId: Int)                       = s"lila/stop $reqId"

  val initialUserIds = Set(UserId("lichess"))
