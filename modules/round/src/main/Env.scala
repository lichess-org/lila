package lila.round

import lila.common.PimpedConfig._
import com.typesafe.config.Config
import akka.actor._

final class Env(
    config: Config,
    system: ActorSystem,
    eloUpdater: lila.user.EloUpdater,
    flood: lila.security.Flood,
    db: lila.db.Env,
    hub: lila.hub.Env,
    ai: lila.ai.Ai,
    i18nKeys: lila.i18n.I18nKeys,
    schedule: Boolean) {

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
  }
  import settings._

  lazy val history = () ⇒ new History(ttl = MessageTtl)

  lazy val socketHub = system.actorOf(Props(new SocketHub(
    makeHistory = history,
    uidTimeout = UidTimeout,
    socketTimeout = SocketTimeout,
    playerTimeout = PlayerTimeout,
    gameSocketName = gameId ⇒ SocketName + "-" + gameId
  )), name = SocketName)

  lazy val socketHandler = new SocketHandler(
    hand = hand,
    socketHub = socketHub,
    messenger = messenger,
    moveNotifier = moveNotifier,
    flood = flood,
    hijack = hijack)

  lazy val hand = new Hand(
    messenger = messenger,
    ai = ai,
    finisher = finisher,
    takeback = takeback,
    socketHub = socketHub,
    moretimeDuration = Moretime)

  lazy val finisher = new Finisher(
    messenger = messenger,
    eloUpdater = eloUpdater,
    eloCalculator = eloCalculator,
    finisherLock = finisherLock,
    indexer = hub.actor.gameIndexer,
    tournamentOrganizer = hub.actor.tournamentOrganizer)

  lazy val meddler = new Meddler(
    finisher = finisher,
    socketHub = socketHub)

  lazy val messenger = new Messenger(i18nKeys)

  val animationDelay = AnimationDelay

  if (schedule) {

    val scheduler = new lila.common.Scheduler(system)
    import scala.concurrent.duration._

    scheduler.future(1.13 hour, "game: finish by clock") {
      titivate.finishByClock
    }

    scheduler.effect(2.3 hour, "game: finish abandoned") {
      titivate.finishAbandoned
    }
  }

  private lazy val titivate = new Titivate(finisher, meddler)

  private lazy val hijack = new Hijack(HijackTimeout)

  private lazy val eloCalculator = new chess.EloCalculator(false)

  private lazy val finisherLock = new FinisherLock(timeout = FinisherLockTimeout)

  private lazy val takeback = new Takeback(messenger)

  private lazy val moveNotifier = new MoveNotifier(
    hub = hub.socket.hub,
    monitor = hub.socket.monitor)

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
    i18nKeys = lila.i18n.Env.current.keys,
    schedule = lila.common.PlayApp.isServer)
}
