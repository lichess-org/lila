package lila.round

import akka.actor._
import akka.pattern.ask
import com.typesafe.config.Config

import lila.common.PimpedConfig._
import lila.hub.actorApi.map.Ask
import lila.socket.actorApi.GetVersion
import makeTimeout.large

final class Env(
    config: Config,
    system: ActorSystem,
    db: lila.db.Env,
    hub: lila.hub.Env,
    ai: lila.ai.Client,
    aiPerfApi: lila.ai.AiPerfApi,
    crosstableApi: lila.game.CrosstableApi,
    lightUser: String => Option[lila.common.LightUser],
    uciMemo: lila.game.UciMemo,
    rematch960Cache: lila.memo.ExpireSetMemo,
    i18nKeys: lila.i18n.I18nKeys,
    prefApi: lila.pref.PrefApi,
    scheduler: lila.common.Scheduler) {

  private val settings = new {
    val MessageTtl = config duration "message.ttl"
    val UidTimeout = config duration "uid.timeout"
    val PlayerDisconnectTimeout = config duration "player.disconnect.timeout"
    val PlayerRagequitTimeout = config duration "player.ragequit.timeout"
    val AnimationDelay = config duration "animation.delay"
    val Moretime = config duration "moretime"
    val SocketName = config getString "socket.name"
    val SocketTimeout = config duration "socket.timeout"
    val FinisherLockTimeout = config duration "finisher.lock.timeout"
    val HijackTimeout = config duration "hijack.timeout"
    val NetDomain = config getString "net.domain"
    val ActorMapName = config getString "actor.map.name"
    val ActorName = config getString "actor.name"
    val HijackEnabled = config getBoolean "hijack.enabled"
    val HijackSalt = config getString "hijack.salt"
    val CollectionReminder = config getString "collection.reminder"
  }
  import settings._

  lazy val history = () => new History(ttl = MessageTtl)

  val roundMap = system.actorOf(Props(new lila.hub.ActorMap[Round] {
    def mkActor(id: String) = new Round(
      gameId = id,
      messenger = messenger,
      takebacker = takebacker,
      finisher = finisher,
      rematcher = rematcher,
      player = player,
      drawer = drawer,
      socketHub = socketHub,
      moretimeDuration = Moretime)
    def receive = actorMapReceive
  }), name = ActorMapName)

  private val socketHub = {
    val actor = system.actorOf(
      Props(new lila.socket.SocketHubActor[Socket] {
        def mkActor(id: String) = new Socket(
          gameId = id,
          history = history(),
          lightUser = lightUser,
          uidTimeout = UidTimeout,
          socketTimeout = SocketTimeout,
          disconnectTimeout = PlayerDisconnectTimeout,
          ragequitTimeout = PlayerRagequitTimeout)
        def receive: Receive = ({
          case msg@lila.chat.actorApi.ChatLine(id, line) =>
            self ! lila.hub.actorApi.map.Tell(id take 8, msg)
          case m: lila.hub.actorApi.game.ChangeFeatured => tellAll(m)
        }: Receive) orElse socketHubReceive
      }),
      name = SocketName)
    system.lilaBus.subscribe(actor, 'changeFeaturedGame)
    actor
  }

  lazy val socketHandler = new SocketHandler(
    hub = hub,
    roundMap = roundMap,
    socketHub = socketHub,
    messenger = messenger,
    hijack = hijack,
    bus = system.lilaBus)

  private lazy val perfsUpdater = new PerfsUpdater

  private lazy val finisher = new Finisher(
    messenger = messenger,
    perfsUpdater = perfsUpdater,
    aiPerfApi = aiPerfApi,
    crosstableApi = crosstableApi,
    reminder = reminder,
    bus = system.lilaBus)

  private lazy val rematcher = new Rematcher(
    messenger = messenger,
    router = hub.actor.router,
    rematch960Cache = rematch960Cache)

  private lazy val player: Player = new Player(
    engine = ai,
    bus = system.lilaBus,
    finisher = finisher,
    cheatDetector = cheatDetector,
    reminder = reminder,
    uciMemo = uciMemo)

  // public access to AI play, for setup.Processor usage
  val aiPlay = player ai _

  private lazy val drawer = new Drawer(
    messenger = messenger,
    finisher = finisher)

  private lazy val cheatDetector = new CheatDetector(reporter = hub.actor.report)

  lazy val meddler = new Meddler(
    roundMap = roundMap,
    socketHub = socketHub)

  lazy val messenger = new Messenger(
    socketHub = socketHub,
    chat = hub.actor.chat,
    i18nKeys = i18nKeys)

  lazy val fenUrlWatch = new FenUrlWatch(
    roundMap = roundMap,
    reporter = hub.actor.report,
    scheduler = system.scheduler)

  def version(gameId: String): Fu[Int] =
    socketHub ? Ask(gameId, GetVersion) mapTo manifest[Int]

  private lazy val reminder = new Reminder(db(CollectionReminder))
  def nowPlaying = reminder.nowPlaying

  private[round] def animationDelay = AnimationDelay
  private[round] def moretimeSeconds = Moretime.toSeconds

  {
    import scala.concurrent.duration._

    scheduler.future(0.23 hour, "game: finish by clock") {
      titivate.finishByClock
    }

    scheduler.effect(0.41 hour, "game: finish abandoned") {
      titivate.finishAbandoned
    }
  }

  private lazy val titivate = new Titivate(roundMap, meddler, scheduler)

  lazy val hijack = new Hijack(HijackTimeout, HijackSalt, HijackEnabled)

  lazy val takebacker = new Takebacker(
    messenger = messenger,
    uciMemo = uciMemo,
    prefApi = prefApi)

  lazy val moveBroadcast = system.actorOf(Props(new MoveBroadcast))
  lazy val tvBroadcast = system.actorOf(Props(new TvBroadcast))
}

object Env {

  lazy val current = "[boot] round" describes new Env(
    config = lila.common.PlayApp loadConfig "round",
    system = lila.common.PlayApp.system,
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    ai = lila.ai.Env.current.client,
    aiPerfApi = lila.ai.Env.current.aiPerfApi,
    crosstableApi = lila.game.Env.current.crosstableApi,
    lightUser = lila.user.Env.current.lightUser,
    uciMemo = lila.game.Env.current.uciMemo,
    rematch960Cache = lila.game.Env.current.cached.rematch960,
    i18nKeys = lila.i18n.Env.current.keys,
    prefApi = lila.pref.Env.current.api,
    scheduler = lila.common.PlayApp.scheduler)
}
