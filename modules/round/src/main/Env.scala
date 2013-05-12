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
  }
  import settings._

  lazy val history = () ⇒ new History(ttl = MessageTtl)

  val socketHub = system.actorOf(Props(new SocketHub(
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
    notifyMove = notifyMove,
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

  private lazy val titivate = new Titivate(finisher, meddler)

  private lazy val hijack = new Hijack(HijackTimeout)

  private lazy val finisherLock = new FinisherLock(timeout = FinisherLockTimeout)

  private lazy val takeback = new Takeback(messenger)

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
    i18nKeys = lila.i18n.Env.current.keys,
    scheduler = lila.common.PlayApp.scheduler)
}
