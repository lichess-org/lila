package lila.pool

import akka.actor._
import akka.pattern.ask
import com.typesafe.config.Config

import lila.common.PimpedConfig._
import lila.hub.actorApi.map.{ Ask, TellAll }
import lila.hub.{ ActorMap, Sequencer }
import lila.socket.actorApi.GetVersion
import lila.socket.History
import makeTimeout.short

final class Env(
    config: Config,
    system: ActorSystem,
    db: lila.db.Env,
    flood: lila.security.Flood,
    lightUser: String => Option[lila.common.LightUser],
    hub: lila.hub.Env,
    roundMap: ActorRef,
    isOnline: String => Boolean,
    secondsToMove: Int,
    scheduler: lila.common.Scheduler) {

  private val settings = new {
    val HistoryMessageTtl = config duration "history.message.ttl"
    val UidTimeout = config duration "uid.timeout"
    val SocketName = config getString "socket.name"
    val OrganizerName = config getString "organizer.name"
  }
  import settings._

  private[pool] val setupRepo = new PoolSetupRepo(config getConfig "presets")

  def setups = setupRepo.setupMap

  lazy val socketHandler = new SocketHandler(
    setupRepo = setupRepo,
    hub = hub,
    poolHub = poolHub,
    chat = hub.actor.chat,
    flood = flood)

  private val poolHub = system.actorOf(
    Props(new lila.socket.SocketHubActor.Default[PoolActor] {
      def mkActor(id: ID) = new PoolActor(
        setup = setupRepo byId id getOrElse {
          throw new IllegalArgumentException(s"Can't create pool for id $id")
        },
        history = new History(ttl = HistoryMessageTtl),
        uidTimeout = UidTimeout,
        lightUser = lightUser,
        isOnline = isOnline,
        joiner = joiner,
        renderer = hub.actor.renderer)
    }), name = SocketName)

  // wake actors up
  setupRepo.setups foreach { setup =>
    poolHub ! lila.hub.actorApi.map.Tell(setup.id, true)
  }

  lazy val repo = new PoolRepo(poolHub, setupRepo)

  lazy val api = new PoolApi(setupRepo, poolHub)

  def version(poolId: String): Fu[Int] =
    poolHub ? Ask(poolId, GetVersion) mapTo manifest[Int]

  private lazy val joiner = new Joiner(
    roundMap = roundMap,
    system = system,
    secondsToMove = secondsToMove)

  {
    import scala.concurrent.duration._

    scheduler.message(1 second) {
      poolHub -> TellAll(actorApi.CheckWave)
    }

    scheduler.message(2 seconds) {
      poolHub -> TellAll(actorApi.CheckPlayers)
    }

    scheduler.message(4 seconds) {
      poolHub -> TellAll(actorApi.RemindPlayers)
    }

    scheduler.message(5 seconds) {
      poolHub -> TellAll(actorApi.EjectLeavers)
    }

    scheduler.message(7 minutes) {
      poolHub -> TellAll(actorApi.CheckLeaders)
    }
  }
}

object Env {

  private def hub = lila.hub.Env.current

  lazy val current = "[boot] pool" describes new Env(
    config = lila.common.PlayApp loadConfig "pool",
    system = lila.common.PlayApp.system,
    db = lila.db.Env.current,
    flood = lila.security.Env.current.flood,
    hub = lila.hub.Env.current,
    lightUser = lila.user.Env.current.lightUser,
    roundMap = lila.round.Env.current.roundMap,
    isOnline = lila.user.Env.current.isOnline,
    secondsToMove = lila.game.Env.current.MandatorySecondsToMove,
    scheduler = lila.common.PlayApp.scheduler)
}
