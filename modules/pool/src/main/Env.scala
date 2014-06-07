package lila.pool

import akka.actor._
import akka.pattern.ask
import com.typesafe.config.Config

import lila.common.PimpedConfig._
import lila.hub.actorApi.map.Ask
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

  private[pool] lazy val poolSetupRepo = new PoolSetupRepo(config getConfig "presets")

  def pools = poolSetupRepo.setups.map { setup =>
    lila.user.UserRepo.byIds(List("thibault", "neio", "clarkey", "hellball")) map { users =>
      Pool(setup, users)
    }
  }.sequenceFu

  lazy val socketHandler = new SocketHandler(
    setupRepo = poolSetupRepo,
    hub = hub,
    socketHub = socketHub,
    chat = hub.actor.chat,
    flood = flood)

  private val socketHub = system.actorOf(
    Props(new lila.socket.SocketHubActor.Default[Socket] {
      def mkActor(poolId: ID) = new Socket(
        poolId = poolId,
        history = new History(ttl = HistoryMessageTtl),
        uidTimeout = UidTimeout,
        lightUser = lightUser)
    }), name = SocketName)

  // private val organizer = system.actorOf(Props(new Organizer(
  //   api = api,
  //   reminder = system.actorOf(Props(new Reminder(
  //     renderer = hub.actor.renderer
  //   )), name = ReminderName),
  //   isOnline = isOnline,
  //   socketHub = socketHub,
  //   evaluator = hub.actor.evaluator
  // )), name = OrganizerName)

  def version(tourId: String): Fu[Int] =
    socketHub ? Ask(tourId, GetVersion) mapTo manifest[Int]

  // private lazy val autoPairing = new AutoPairing(roundMap = roundMap, system = system)

  // {
  //   import scala.concurrent.duration._

  // scheduler.message(2 seconds) {
  //   organizer -> actorApi.AllCreatedTournaments
  // }

  // scheduler.message(3 seconds) {
  //   organizer -> actorApi.StartedTournaments
  // }

  // scheduler.message(6 minutes) {
  //   organizer -> actorApi.CheckLeaders
  // }
  // }
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
