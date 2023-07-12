package lila.socket

import akka.actor.{ CoordinatedShutdown, Scheduler }
import chess.{ Centis, Color }
import io.lettuce.core.*
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection as PubSub
import java.util.concurrent.atomic.AtomicReference
import play.api.libs.json.*
import scala.util.chaining.*
import lila.socket.Socket.Sri

import lila.common.{ Bus, Lilakka }
import lila.hub.actorApi.Announce
import lila.hub.actorApi.relation.{ Follow, UnFollow }
import lila.hub.actorApi.round.Mlat
import lila.hub.actorApi.security.CloseAccount
import lila.hub.actorApi.socket.remote.{ TellSriIn, TellSriOut, TellSrisOut, TellUserIn }
import lila.hub.actorApi.socket.{ ApiUserIsOnline, SendTo, SendToOnlineUser, SendTos }

final class RemoteSocket(redisClient: RedisClient, shutdown: CoordinatedShutdown)(using Executor, Scheduler):

  import RemoteSocket.*, Protocol.*

  private var stopping = false

  private type UserIds = Set[UserId]

  val onlineUserIds: AtomicReference[Set[UserId]] = AtomicReference(initialUserIds)

  val baseHandler: Handler =
    case In.ConnectUser(userId) =>
      onlineUserIds.getAndUpdate(_ + userId)
    case In.ConnectUsers(userIds) =>
      onlineUserIds.getAndUpdate(_ ++ userIds)
    case In.DisconnectUsers(userIds) =>
      onlineUserIds.getAndUpdate(_ -- userIds)
    case In.NotifiedBatch(userIds) =>
      Bus.publish(lila.hub.actorApi.notify.NotifiedBatch(userIds), "notify")
    case In.Lags(lags) =>
      lags.foreach: (userId, centis) =>
        UserLagCache.put(userId, centis)
      // this shouldn't be necessary... ensure that users are known to be online
      onlineUserIds.getAndUpdate((x: UserIds) => x ++ lags.keys)
    case In.TellSri(sri, userId, typ, msg) =>
      Bus.publish(TellSriIn(sri.value, userId, msg), s"remoteSocketIn:$typ")
    case In.TellUser(userId, typ, msg) =>
      Bus.publish(TellUserIn(userId, msg), s"remoteSocketIn:$typ")
    case In.ReqResponse(reqId, response) => SocketRequest.onResponse(reqId, response)
    case In.Ping(id)                     => send(Out.pong(id))
    case In.WsBoot =>
      logger.warn("Remote socket boot")
      onlineUserIds set initialUserIds

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
  ) {
    case SendTos(userIds, payload) =>
      val connectedUsers = userIds intersect onlineUserIds.get
      if connectedUsers.nonEmpty then send(Out.tellUsers(connectedUsers, payload))
    case SendTo(userId, payload) =>
      if onlineUserIds.get.contains(userId) then send(Out.tellUser(userId, payload))
    case SendToOnlineUser(userId, makePayload) =>
      if onlineUserIds.get.contains(userId) then
        makePayload() foreach { payload =>
          send(Out.tellUser(userId, payload))
        }
    case Announce(_, _, json) =>
      send(Out.tellAll(Json.obj("t" -> "announce", "d" -> json)))
    case Mlat(millis) =>
      send(Out.mlat(millis))
    case SendToFlag(flag, payload) =>
      send(Out.tellFlag(flag, payload))
    case TellSriOut(sri, payload) =>
      send(Out.tellSri(Sri(sri), payload))
    case TellSrisOut(sris, payload) =>
      send(Out.tellSris(Sri from sris, payload))
    case CloseAccount(userId) =>
      send(Out.disconnectUser(userId))
    case lila.hub.actorApi.mod.Shadowban(userId, v) =>
      send(Out.setTroll(userId, v))
    case lila.hub.actorApi.mod.Impersonate(userId, modId) =>
      send(Out.impersonate(userId, modId))
    case ApiUserIsOnline(userId, value) =>
      send(Out.apiUserOnline(userId, value))
      if value then onlineUserIds.getAndUpdate(_ + userId)
    case Follow(u1, u2)   => send(Out.follow(u1, u2))
    case UnFollow(u1, u2) => send(Out.unfollow(u1, u2))
  }

  final class StoppableSender(val conn: PubSub[String, String], channel: Channel) extends Sender:
    def apply(msg: String)               = if !stopping then super.send(channel, msg)
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

  private val send: Send = makeSender("site-out").apply

  def subscribe(channel: Channel, reader: In.Reader)(handler: Handler): Funit =
    connectAndSubscribe(channel): str =>
      RawMsg(str) match
        case None => logger.error(s"Invalid $channel $str")
        case Some(msg) =>
          reader(msg) collect handler match
            case Some(_) => // processed
            case None    => logger.warn(s"Unhandled $channel $str")

  def subscribeRoundRobin(channel: Channel, reader: In.Reader, parallelism: Int)(
      handler: Handler
  ): Funit =
    // subscribe to main channel
    subscribe(channel, reader)(handler) >> {
      // and subscribe to subchannels
      (0 to parallelism)
        .map: index =>
          subscribe(s"$channel:$index", reader)(handler)
        .parallel
        .void
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
    SocketRequest[Unit](
      id => send(Protocol.Out.stop(id)),
      res => logger.info(s"lila-ws says: $res")
    ).withTimeout(1 second, "Lilakka.shutdown")
      .addFailureEffect(e => logger.error("lila-ws stop", e))
      .recoverDefault

  Lilakka.shutdown(shutdown, _.PhaseServiceUnbind, "Stopping the socket redis pool"): () =>
    Future:
      stopping = true
      redisClient.shutdown()

object RemoteSocket:

  type Send = String => Unit

  trait Sender:
    def apply(msg: String): Unit
    def sticky(_id: String, msg: String): Unit

    protected val conn: PubSub[String, String]
    protected def send(channel: Channel, msg: String) = conn.async.publish(channel, msg)

  object Protocol:

    final class RawMsg(val path: String, val args: Args):
      def get(nb: Int)(f: PartialFunction[Array[String], Option[In]]): Option[In] =
        f.applyOrElse(args.split(" ", nb), (_: Array[String]) => None)
      def all = args split ' '
    def RawMsg(msg: String): Option[RawMsg] =
      val parts = msg.split(" ", 2)
      parts.headOption map {
        new RawMsg(_, ~parts.lift(1))
      }

    trait In
    object In:

      type Reader = RawMsg => Option[In]

      case object WsBoot                                                               extends In
      case class ConnectUser(userId: UserId)                                           extends In
      case class ConnectUsers(userIds: Iterable[UserId])                               extends In
      case class DisconnectUsers(userIds: Iterable[UserId])                            extends In
      case class ConnectSris(cons: Iterable[(Sri, Option[UserId])])                    extends In
      case class DisconnectSris(sris: Iterable[Sri])                                   extends In
      case class NotifiedBatch(userIds: Iterable[UserId])                              extends In
      case class Lag(userId: UserId, lag: Centis)                                      extends In
      case class Lags(lags: Map[UserId, Centis])                                       extends In
      case class TellSri(sri: Sri, userId: Option[UserId], typ: String, msg: JsObject) extends In
      case class TellUser(userId: UserId, typ: String, msg: JsObject)                  extends In
      case class ReqResponse(reqId: Int, response: String)                             extends In
      case class Ping(id: String)                                                      extends In

      val baseReader: Reader = raw =>
        raw.path match
          case "connect/user"     => ConnectUser(UserId(raw.args)).some
          case "connect/users"    => ConnectUsers(UserId.from(commas(raw.args))).some
          case "disconnect/users" => DisconnectUsers(UserId from commas(raw.args)).some
          case "connect/sris" =>
            ConnectSris {
              commas(raw.args) map (_ split ' ') map { s =>
                (Sri(s(0)), UserId.from(s lift 1))
              }
            }.some
          case "disconnect/sris" => DisconnectSris(commas(raw.args) map { Sri(_) }).some
          case "notified/batch"  => NotifiedBatch(UserId from commas(raw.args)).some
          case "lag" =>
            raw.all pipe { s =>
              Centis.from(s lift 1 flatMap (_.toIntOption)) map { Lag(UserId(s(0)), _) }
            }
          case "lags" =>
            Lags(commas(raw.args).flatMap {
              _ split ':' match
                case Array(user, l) =>
                  l.toIntOption map { lag =>
                    UserId(user) -> Centis(lag)
                  }
                case _ => None
            }.toMap).some
          case "tell/sri" => raw.get(3)(tellSriMapper)
          case "tell/user" =>
            raw.get(2) { case Array(user, payload) =>
              for
                obj <- Json.parse(payload).asOpt[JsObject]
                typ <- obj str "t"
              yield TellUser(UserId(user), typ, obj)
            }
          case "req/response" =>
            raw.get(2) { case Array(reqId, response) =>
              reqId.toIntOption map { ReqResponse(_, response) }
            }
          case "ping" => Ping(raw.args).some
          case "boot" => WsBoot.some
          case _      => none

      def tellSriMapper: PartialFunction[Array[String], Option[TellSri]] = { case Array(sri, user, payload) =>
        for
          obj <- Json.parse(payload).asOpt[JsObject]
          typ <- obj str "t"
        yield TellSri(Sri(sri), UserId from optional(user), typ, obj)
      }

      def commas(str: String): Array[String]    = if str == "-" then Array.empty else str split ','
      def boolean(str: String): Boolean         = str == "+"
      def optional(str: String): Option[String] = if str == "-" then None else Some(str)

    object Out:
      def tellUser(userId: UserId, payload: JsObject) =
        s"tell/users $userId ${Json stringify payload}"
      def tellUsers(userIds: Set[UserId], payload: JsObject) =
        s"tell/users ${commas(userIds)} ${Json stringify payload}"
      def tellAll(payload: JsObject) =
        s"tell/all ${Json stringify payload}"
      def tellFlag(flag: String, payload: JsObject) =
        s"tell/flag $flag ${Json stringify payload}"
      def tellSri(sri: Sri, payload: JsValue) =
        s"tell/sri $sri ${Json stringify payload}"
      def tellSris(sris: Iterable[Sri], payload: JsValue) =
        s"tell/sris ${commas(sris)} ${Json stringify payload}"
      def mlat(millis: Int) =
        s"mlat ${millis}"
      def disconnectUser(userId: UserId) =
        s"disconnect/user $userId"
      def setTroll(userId: UserId, v: Boolean) =
        s"mod/troll/set $userId ${boolean(v)}"
      def impersonate(userId: UserId, by: Option[UserId]) =
        s"mod/impersonate $userId ${optional(by.map(_.value))}"
      def follow(u1: UserId, u2: UserId)         = s"rel/follow $u1 $u2"
      def unfollow(u1: UserId, u2: UserId)       = s"rel/unfollow $u1 $u2"
      def apiUserOnline(u: UserId, v: Boolean)   = s"api/online $u ${boolean(v)}"
      def respond(reqId: Int, payload: JsObject) = s"req/response $reqId ${Json stringify payload}"
      def boot                                   = "boot"
      def pong(id: String)                       = s"pong $id"
      def stop(reqId: Int)                       = s"lila/stop $reqId"

      def commas(strs: Iterable[Any]): String = if strs.isEmpty then "-" else strs mkString ","
      def boolean(v: Boolean): String         = if v then "+" else "-"
      def optional(str: Option[String])       = str getOrElse "-"
      def color(c: Color): String             = c.fold("w", "b")
      def color(c: Option[Color]): String     = optional(c.map(_.fold("w", "b")))

  val initialUserIds = Set(UserId("lichess"))

  type Channel = String
  type Args    = String
  type Handler = PartialFunction[Protocol.In, Unit]
