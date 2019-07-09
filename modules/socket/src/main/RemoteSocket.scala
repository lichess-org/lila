package lila.socket

import play.api.libs.json._
import redis.clients.jedis._
import scala.concurrent.{ Future, blocking }

import chess.Centis
import lila.common.WithResource
import lila.hub.actorApi.round.{ MoveEvent, FinishGameId, Mlat }
import lila.hub.actorApi.socket.{ SendTo, SendTos, WithUserIds }
import lila.hub.actorApi.{ Deploy, Announce }

private final class RemoteSocket(
    redisPool: JedisPool,
    chanIn: String,
    chanOut: String,
    lifecycle: play.api.inject.ApplicationLifecycle,
    notificationActor: akka.actor.ActorSelection,
    setNb: Int => Unit,
    bus: lila.common.Bus
) {

  private object In {
    val Connect = "connect"
    val Disconnect = "disconnect"
    val Watch = "watch"
    val Notified = "notified"
    val Connections = "connections"
    val Lag = "lag"
  }
  private object Out {
    val Move = "move"
    val TellUser = "tell/user"
    val TellUsers = "tell/users"
    val TellAll = "tell/all"
    val TellFlag = "tell/flag"
    val Mlat = "mlat"
  }

  private val connectedUserIds = collection.mutable.Set.empty[String]
  private val watchedGameIds = collection.mutable.Set.empty[String]

  bus.subscribeFun('moveEvent, 'finishGameId, 'socketUsers, 'deploy, 'announce, 'mlat, 'sendToFlag) {
    case MoveEvent(gameId, fen, move) =>
      if (watchedGameIds(gameId)) send(Out.Move, gameId, move, fen)
    case FinishGameId(gameId) if watchedGameIds(gameId) =>
      watchedGameIds -= gameId
    case SendTos(userIds, payload) =>
      val connectedUsers = userIds intersect connectedUserIds
      if (connectedUsers.nonEmpty) send(Out.TellUsers, connectedUsers mkString ",", Json stringify payload)
    case SendTo(userId, payload) if connectedUserIds(userId) =>
      send(Out.TellUser, userId, Json stringify payload)
    case d: Deploy =>
      send(Out.TellAll, Json stringify Json.obj("t" -> d.key))
    case Announce(msg) =>
      send(Out.TellAll, Json stringify Json.obj("t" -> "announce", "d" -> Json.obj("msg" -> msg)))
    case Mlat(ms) =>
      send(Out.Mlat, ms.toString)
    case actorApi.SendToFlag(flag, payload) =>
      send(Out.TellFlag, flag, Json stringify payload)
    case WithUserIds(f) =>
      f(connectedUserIds)
  }

  private def onReceive(path: String, args: String) = path match {
    case In.Connect =>
      val userId = args
      connectedUserIds += userId
    case In.Disconnect =>
      val userId = args
      connectedUserIds -= args
    case In.Watch =>
      val gameId = args
      watchedGameIds += gameId
    case In.Notified =>
      val userId = args
      notificationActor ! lila.hub.actorApi.notify.Notified(userId)
    case In.Connections =>
      parseIntOption(args) foreach { nb =>
        setNb(nb)
        tick(nb)
      }
    case In.Lag => args split ' ' match {
      case Array(user, l) => parseIntOption(l) foreach { lag =>
        UserLagCache.put(user, Centis(lag))
      }
      case _ =>
    }
    case path =>
      logger.warn(s"Invalid path $path")
  }

  /* Can fail if redis is offline, but must not propagate the exception,
   * because subscribeFun is synchronous, and this runs on the same thread
   * as the code that triggered the event */
  private def send(path: String, args: String*): Unit = {
    redisMon.out()
    lila.common.Chronometer.syncMon(_.socket.remote.redis.publishTime) {
      executeBlockingIO {
        WithResource(redisPool.getResource) { redis =>
          redis.publish(chanOut, s"$path ${args mkString " "}")
        }
      }
    }
  }

  private def tick(nbConn: Int): Unit = {
    // println(nbIn, "nbIn")
    mon.connections(nbConn)
    mon.sets.users(connectedUserIds.size)
    mon.sets.games(watchedGameIds.size)
    // mon.executor.threads(ioThreadCounter.get.pp("threads"))
    redisMon.pool.active(redisPool.getNumActive)
    redisMon.pool.idle(redisPool.getNumIdle)
    redisMon.pool.waiters(redisPool.getNumWaiters)
  }

  private val mon = lila.mon.socket.remote
  private val redisMon = mon.redis

  // private var nbIn = 0

  private def subscribe: Funit = Future {
    redisPool.getResource.subscribe(new JedisPubSub() {
      override def onMessage(channel: String, message: String): Unit = {
        // nbIn += 1
        // if (channel == "bench") {
        // lettuce:
        // (1200000,3407)
        // (1300000,3441)
        // (1400000,3458)
        // jedis:
        // (1200000,1988)
        // (1300000,2023)
        // (1400000,2112)
        // blocking { jedis }
        // (1200000,2277)
        // (1300000,2256)
        // (1400000,2237)
        // io executor { jedis }
        // (1200000,2640)
        // (1300000,2631)
        // (1400000,2641)
        // it = it + 1
        // if (it % 100000 == 0) {
        //   println(it, (nowMillis - last).toString)
        //   last = nowMillis
        // }
        // redisMon.out()
        // executeBlockingIO {
        //   WithResource(redisPool.getResource) { redis =>
        //     redis.publish("bench", "tagada")
        //   }
        // }
        // } else {
        val parts = message.split(" ", 2)
        // println(parts(0), ~parts.lift(1))
        onReceive(parts(0), ~parts.lift(1))
        redisMon.in()
        // }
      }
      // }, chanIn, "bench")
    }, chanIn)
  }.void logFailure logger addEffectAnyway subscribe

  subscribe

  // var it = 0
  // var last = nowMillis

  private def executeBlockingIO[T](cb: => T): Unit = try {
    blocking(cb)
  } catch {
    case scala.util.control.NonFatal(e) =>
      logger.warn(s"RemoteSocket.out", e)
      redisMon.outError()
  }

  // import java.util.concurrent.{ Executors, ThreadFactory }
  // import java.util.concurrent.atomic.AtomicLong
  // private val ioThreadCounter = new AtomicLong(0L)
  // private val ioThreadPool = Executors.newCachedThreadPool(
  //   new ThreadFactory {
  //     def newThread(r: Runnable) = {
  //       val th = new Thread(r)
  //       th.setName(s"remote-socket-redis-${ioThreadCounter.getAndIncrement}")
  //       th.setDaemon(true)
  //       th
  //     }
  //   }
  // )

  // private def executeBlockingIO[T](cb: => T): Unit = {
  //   ioThreadPool.execute(new Runnable {
  //     def run() = try {
  //       blocking(cb)
  //     } catch {
  //       case scala.util.control.NonFatal(e) =>
  //         logger.warn(s"RemoteSocket.out", e)
  //         redisMon.outError()
  //     }
  //   })
  // }

  lifecycle.addStopHook { () =>
    logger.info("Stopping the Redis pool...")
    Future {
      // ioThreadPool.shutdown()
      redisPool.close()
    }
  }
}
