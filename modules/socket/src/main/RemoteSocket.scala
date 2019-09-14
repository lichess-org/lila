package lila.socket

import chess.Centis
import io.lettuce.core._
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import play.api.libs.json._
import scala.concurrent.Future

import lila.common.Chronometer
import lila.hub.actorApi.relation.ReloadOnlineFriends
import lila.hub.actorApi.round.{ MoveEvent, Mlat }
import lila.hub.actorApi.security.CloseAccount
import lila.hub.actorApi.socket.remote.{ TellSriIn, TellSriOut }
import lila.hub.actorApi.socket.{ SendTo, SendTos, WithUserIds }
import lila.hub.actorApi.{ Deploy, Announce }
import lila.socket.actorApi.SendToFlag
import Socket.Sri

final class RemoteSocket(
    redisClient: RedisClient,
    notificationActor: akka.actor.ActorSelection,
    setNb: Int => Unit,
    bus: lila.common.Bus,
    lifecycle: play.api.inject.ApplicationLifecycle
) {

  import RemoteSocket._, Protocol._

  private val connectedUserIds = collection.mutable.Set.empty[String]
  private val watchedGameIds = collection.mutable.Set.empty[String]

  private def connectToPubSub = redisClient.connectPubSub()

  val baseHandler: Handler = {
    case In.ConnectUser(userId) =>
      bus.publish(lila.hub.actorApi.socket.remote.ConnectUser(userId), 'userActive)
      connectedUserIds += userId
    case In.DisconnectUsers(userIds) => connectedUserIds --= userIds
    case In.Watch(gameId) => watchedGameIds += gameId
    case In.Unwatch(gameId) => watchedGameIds -= gameId
    case In.NotifiedBatch(userIds) => notificationActor ! lila.hub.actorApi.notify.NotifiedBatch(userIds)
    case In.Connections(nb) => tick(nb)
    case In.FriendsBatch(userIds) => userIds foreach { userId =>
      bus.publish(ReloadOnlineFriends(userId), 'reloadOnlineFriends)
    }
    case In.Lags(lags) => lags foreach (UserLagCache.put _).tupled
    case In.TellSri(sri, userId, typ, msg) =>
      bus.publish(TellSriIn(sri.value, userId, msg), Symbol(s"remoteSocketIn:$typ"))
    case In.DisconnectAll =>
      logger.info("Remote socket disconnect all")
      connectedUserIds.clear
      watchedGameIds.clear
  }

  bus.subscribeFun('moveEvent, 'socketUsers, 'deploy, 'announce, 'mlat, 'sendToFlag, 'remoteSocketOut, 'accountClose) {
    case MoveEvent(gameId, fen, move) =>
      if (watchedGameIds(gameId)) send(Out.move(gameId, move, fen))
    case SendTos(userIds, payload) =>
      val connectedUsers = userIds intersect connectedUserIds
      if (connectedUsers.nonEmpty) send(Out.tellUsers(connectedUsers, payload))
    case SendTo(userId, payload) if connectedUserIds(userId) =>
      send(Out.tellUser(userId, payload))
    case d: Deploy =>
      send(Out.tellAll(Json.obj("t" -> d.key)))
    case Announce(msg) =>
      send(Out.tellAll(Json.obj("t" -> "announce", "d" -> Json.obj("msg" -> msg))))
    case Mlat(micros) =>
      send(Out.mlat(micros))
    case actorApi.SendToFlag(flag, payload) =>
      send(Out.tellFlag(flag, payload))
    case TellSriOut(sri, payload) =>
      send(Out.tellSri(Sri(sri), payload))
    case CloseAccount(userId) =>
      send(Out.disconnectUser(userId))
    case WithUserIds(f) =>
      f(connectedUserIds)
  }

  private def tick(nbConn: Int): Unit = {
    setNb(nbConn)
    mon.connections(nbConn)
    mon.sets.users(connectedUserIds.size)
    mon.sets.games(watchedGameIds.size)
  }

  private val mon = lila.mon.socket.remote

  def makeSender(channel: Channel): Sender = new Sender(connectToPubSub, channel)

  private val send: String => Unit = makeSender("site-out").apply _

  def subscribe(channel: Channel, reader: In.Reader)(handler: Handler): Unit = {
    val conn = connectToPubSub
    conn.addListener(new pubsub.RedisPubSubAdapter[String, String] {
      override def message(_channel: String, message: String): Unit = {
        val raw = RawMsg(message)
        mon.redis.in.channel(channel)()
        mon.redis.in.path(channel, raw.path)()
        // println(message, s"in:$channel")
        reader(raw) collect handler match {
          case Some(_) => // processed
          case None => logger.warn(s"Unhandled $message")
        }
      }
    })
    conn.async.subscribe(channel)
  }

  lifecycle.addStopHook { () =>
    logger.info("Stopping the Redis pool...")
    Future {
      redisClient.shutdown()
    }
  }
}

object RemoteSocket {

  final class Sender(conn: StatefulRedisPubSubConnection[String, String], channel: Channel) {

    private val mon = lila.mon.socket.remote

    def apply(msg: String): Unit = {
      val chrono = Chronometer.start
      Chronometer.syncMon(_.socket.remote.redis.publishTimeSync) {
        // println(msg, s"out:$channel")
        conn.async.publish(channel, msg).thenRun {
          new Runnable { def run = chrono.mon(_.socket.remote.redis.publishTime) }
        }
      }
      mon.redis.out.channel(channel)()
      mon.redis.out.path(channel, msg.takeWhile(' ' !=))()
    }
  }

  object Protocol {

    case class RawMsg(path: Path, args: Args)
    def RawMsg(msg: String): RawMsg = {
      val parts = msg.split(" ", 2)
      RawMsg(parts(0), ~parts.lift(1))
    }

    trait In
    object In {

      type Reader = RawMsg => Option[In]

      case class ConnectUser(userId: String) extends In
      case class DisconnectUsers(userId: Iterable[String]) extends In
      case class ConnectSri(sri: Sri, userId: Option[String]) extends In // deprecated #TODO remove me
      case class ConnectSris(cons: Iterable[(Sri, Option[String])]) extends In
      case class DisconnectSri(sri: Sri) extends In // deprecated #TODO remove me
      case class DisconnectSris(sris: Iterable[Sri]) extends In
      case object DisconnectAll extends In
      case class Watch(gameId: String) extends In
      case class Unwatch(gameId: String) extends In
      case class NotifiedBatch(userIds: Iterable[String]) extends In
      case class Connections(nb: Int) extends In
      case class Lag(userId: String, lag: Centis) extends In
      case class Lags(lags: Map[String, Centis]) extends In
      case class FriendsBatch(userIds: Iterable[String]) extends In
      case class TellSri(sri: Sri, userId: Option[String], typ: String, msg: JsObject) extends In

      val baseReader: Reader = raw => raw.path match {
        case "connect/user" => ConnectUser(raw.args).some
        case "disconnect/users" => DisconnectUsers(raw.args split ',').some
        case "connect/sri" => raw.args.split(' ') |> { s => ConnectSri(Sri(s(0)), s lift 1).some }
        case "connect/sris" => ConnectSris {
          raw.args.split(',') map (_ split ' ') map { s =>
            (Sri(s(0)), s lift 1)
          }
        }.some
        case "disconnect/sri" => DisconnectSri(Sri(raw.args)).some
        case "disconnect/sris" => DisconnectSris(raw.args split ',' map Sri.apply).some
        case "disconnect/all" => DisconnectAll.some
        case "watch" => Watch(raw.args).some
        case "unwatch" => Unwatch(raw.args).some
        case "notified/batch" => NotifiedBatch(raw.args split ' ').some
        case "connections" => parseIntOption(raw.args) map Connections.apply
        case "lag" => raw.args.split(' ') |> { s => s lift 1 flatMap parseIntOption map Centis.apply map { Lag(s(0), _) } }
        case "lags" => Lags(raw.args.split(',').flatMap {
          _ split ':' match {
            case Array(user, l) => parseIntOption(l) map { lag => user -> Centis(lag) }
            case _ => None
          }
        }.toMap).some
        case "friends/batch" => FriendsBatch(raw.args split ',').some
        case "tell/sri" => raw.args.split(" ", 3) match {
          case Array(sri, userOrAnon, payload) => for {
            obj <- Json.parse(payload).asOpt[JsObject]
            typ <- obj str "t"
            userId = userOrAnon.some.filter("-" !=)
          } yield TellSri(Sri(sri), userId, typ, obj)
          case _ => none
        }
        case _ => none
      }
    }

    object Out {
      def move(gameId: String, move: String, fen: String) =
        s"move $gameId $move $fen"
      def tellUser(userId: String, payload: JsObject) =
        s"tell/users $userId ${Json stringify payload}"
      def tellUsers(userIds: Set[String], payload: JsObject) =
        s"tell/users ${commaList(userIds)} ${Json stringify payload}"
      def tellAll(payload: JsObject) =
        s"tell/all ${Json stringify payload}"
      def tellFlag(flag: String, payload: JsObject) =
        s"tell/flag $flag ${Json stringify payload}"
      def tellSri(sri: Sri, payload: JsValue) =
        s"tell/sri $sri ${Json stringify payload}"
      def mlat(micros: Int) =
        s"mlat ${((micros / 100) / 10d)}"
      def disconnectUser(userId: String) =
        s"disconnect/user $userId"

      def commaList(strs: Iterable[Any]) =
        if (strs.isEmpty) "-"
        else strs mkString ","
    }
  }

  type Channel = String
  type Path = String
  type Args = String
  type Handler = PartialFunction[Protocol.In, Unit]
}
