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
    scheduler: lila.common.Scheduler) {

  private val settings = new {
    val HistoryMessageTtl = config duration "history.message.ttl"
    val UidTimeout = config duration "uid.timeout"
    val SocketName = config getString "socket.name"
    val OrganizerName = config getString "organizer.name"
  }
  import settings._

  private[pool] val poolSetupRepo = new PoolSetupRepo(config getConfig "presets")

  lazy val socketHandler = new SocketHandler(
    setupRepo = poolSetupRepo,
    hub = hub,
    poolHub = poolHub,
    chat = hub.actor.chat,
    flood = flood)

  private val poolHub = system.actorOf(
    Props(new lila.socket.SocketHubActor.Default[PoolActor] {
      def mkActor(id: ID) = new PoolActor(
        setup = poolSetupRepo byId id getOrElse {
          throw new IllegalArgumentException(s"Can't create pool for id $id")
        },
        history = new History(ttl = HistoryMessageTtl),
        uidTimeout = UidTimeout,
        lightUser = lightUser,
        renderer = hub.actor.renderer)
    }), name = SocketName)

  poolSetupRepo.setups foreach { setup =>
    poolHub ! lila.hub.actorApi.map.Tell(setup.id, true)
  }

  lazy val repo = new PoolRepo(poolHub)

  lazy val api = new PoolApi(poolHub)

  // private val organizer = system.actorOf(Props(new Organizer(
  //   api = api,
  //   reminder = system.actorOf(Props(new Reminder(
  //     renderer = hub.actor.renderer
  //   )), name = ReminderName),
  //   isOnline = isOnline,
  //   poolHub = poolHub,
  //   evaluator = hub.actor.evaluator
  // )), name = OrganizerName)

  def version(poolId: String): Fu[Int] =
    poolHub ? Ask(poolId, GetVersion) mapTo manifest[Int]

  // private lazy val autoPairing = new AutoPairing(roundMap = roundMap, system = system)

  {
    import scala.concurrent.duration._

    scheduler.message(3 seconds) {
      poolHub -> TellAll(actorApi.Pairing)
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
    scheduler = lila.common.PlayApp.scheduler)
}
