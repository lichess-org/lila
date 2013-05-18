package lila.round

import lila.common.PimpedConfig._
import lila.socket.actorApi.{ Forward, GetVersion }
import makeTimeout.large

import com.typesafe.config.Config
import akka.actor._
import akka.pattern.ask

final class Env(
    config: Config,
    system: ActorSystem,
    eloUpdater: lila.user.EloUpdater,
    flood: lila.security.Flood,
    db: lila.db.Env,
    hub: lila.hub.Env,
    ai: lila.ai.Ai,
    getUsername: String ⇒ Fu[Option[String]],
    i18nKeys: lila.i18n.I18nKeys,
    scheduler: lila.common.Scheduler) {

  private val settings = new {
    val MessageTtl = config duration "message.ttl"
    val UidTimeout = config duration "uid.timeout"
    val PlayerTimeout = config duration "player.timeout"
    val AnimationDelay = config duration "animation.delay"
    val Moretime = config duration "moretime"
    val CollectionRoom = config getString "collection.room"
    val CollectionWatcherRoom = config getString "collection.watcher_room"
    val SocketName = config getString "socket.name"
    val SocketTimeout = config duration "socket.timeout"
    val FinisherLockTimeout = config duration "finisher.lock.timeout"
    val HijackTimeout = config duration "hijack.timeout"
    val NetDomain = config getString "net.domain"
    val ActorMapName = config getString "actor.map.name"
  }
  import settings._

  lazy val history = () ⇒ new History(ttl = MessageTtl)

  val roundMap = system.actorOf(Props(new lila.hub.ActorMap(id ⇒
    new Round(
      gameId = id,
      messenger = messenger,
      takebacker = takebacker,
      ai = ai,
      finisher = finisher,
      rematcher = rematcher,
      drawer = drawer,
      socketHub = socketHub,
      moretimeDuration = Moretime)
  )), name = ActorMapName)

  val socketHub = system.actorOf(Props(new SocketHub(
    makeHistory = history,
    getUsername = getUsername,
    uidTimeout = UidTimeout,
    socketTimeout = SocketTimeout,
    playerTimeout = PlayerTimeout
  )), name = SocketName)

  lazy val socketHandler = new SocketHandler(
    roundMap = roundMap,
    socketHub = socketHub,
    messenger = messenger,
    notifyMove = notifyMove,
    flood = flood,
    hijack = hijack)

  private lazy val finisher = new Finisher(
    messenger = messenger,
    eloUpdater = eloUpdater,
    eloCalculator = eloCalculator,
    indexer = hub.actor.gameIndexer,
    tournamentOrganizer = hub.actor.tournamentOrganizer)

  private lazy val rematcher = new Rematcher(
    messenger = messenger,
    router = hub.actor.router,
    timeline = hub.actor.timeline)

  private lazy val drawer = new Drawer(
    messenger = messenger,
    finisher = finisher)

  lazy val meddler = new Meddler(
    roundMap = roundMap,
    socketHub = socketHub)

  lazy val messenger = new Messenger(NetDomain, i18nKeys, getUsername)

  lazy val eloCalculator = new chess.EloCalculator(false)

  def version(gameId: String): Fu[Int] =
    socketHub ? Forward(gameId, GetVersion) mapTo manifest[Int]

  def animationDelay = AnimationDelay
  def moretimeSeconds = Moretime.toSeconds

  {
    import scala.concurrent.duration._

    scheduler.future(1.13 hour, "game: finish by clock") {
      titivate.finishByClock
    }

    scheduler.effect(2.3 hour, "game: finish abandoned") {
      titivate.finishAbandoned
    }
  }

  private lazy val titivate = new Titivate(roundMap, meddler)

  private lazy val hijack = new Hijack(HijackTimeout)

  private lazy val takebacker = new Takebacker(
    messenger = messenger)

  private def notifyMove(gameId: String, fen: String, lastMove: Option[String]) {
    hub.socket.hub ! lila.socket.actorApi.Fen(gameId, fen, lastMove)
    hub.socket.monitor ! lila.hub.actorApi.monitor.AddMove
  }

  private[round] lazy val roomColl = db(CollectionRoom)

  private[round] lazy val watcherRoomColl = db(CollectionWatcherRoom)
}

object Env {

  lazy val current = "[boot] round" describes new Env(
    config = lila.common.PlayApp loadConfig "round",
    system = lila.common.PlayApp.system,
    eloUpdater = lila.user.Env.current.eloUpdater,
    flood = lila.security.Env.current.flood,
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    ai = lila.ai.Env.current.ai,
    getUsername = lila.user.Env.current.usernameOption,
    i18nKeys = lila.i18n.Env.current.keys,
    scheduler = lila.common.PlayApp.scheduler)
}
