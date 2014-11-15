package lila.round

import akka.actor._
import akka.pattern.ask
import com.typesafe.config.Config
import scala.concurrent.duration._

import actorApi.{ GetSocketStatus, SocketStatus }
import lila.common.PimpedConfig._
import lila.hub.actorApi.map.Ask
import lila.memo.AsyncCache
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
    userJsonView: lila.user.JsonView,
    uciMemo: lila.game.UciMemo,
    rematch960Cache: lila.memo.ExpireSetMemo,
    onStart: String => Unit,
    i18nKeys: lila.i18n.I18nKeys,
    prefApi: lila.pref.PrefApi,
    chatApi: lila.chat.ChatApi,
    historyApi: lila.history.HistoryApi,
    scheduler: lila.common.Scheduler) {

  private val settings = new {
    val MessageTtl = config duration "message.ttl"
    val UidTimeout = config duration "uid.timeout"
    val PlayerDisconnectTimeout = config duration "player.disconnect.timeout"
    val PlayerRagequitTimeout = config duration "player.ragequit.timeout"
    val AnimationDuration = config duration "animation.duration"
    val Moretime = config duration "moretime"
    val SocketName = config getString "socket.name"
    val SocketTimeout = config duration "socket.timeout"
    val FinisherLockTimeout = config duration "finisher.lock.timeout"
    val NetDomain = config getString "net.domain"
    val ActorMapName = config getString "actor.map.name"
    val ActorName = config getString "actor.name"
    val CollectionReminder = config getString "collection.reminder"
    val CasualOnly = config getBoolean "casual_only"
    val ActiveTtl = config duration "active.ttl"
  }
  import settings._

  lazy val history = () => new History(ttl = MessageTtl)

  val roundMap = system.actorOf(Props(new lila.hub.ActorMap {
    def mkActor(id: String) = new Round(
      gameId = id,
      messenger = messenger,
      takebacker = takebacker,
      finisher = finisher,
      rematcher = rematcher,
      player = player,
      drawer = drawer,
      socketHub = socketHub,
      moretimeDuration = Moretime,
      activeTtl = ActiveTtl)
    def receive: Receive = ({
      case actorApi.BroadcastSize => hub.socket.lobby ! lila.hub.actorApi.round.NbRounds(size)
    }: Receive) orElse actorMapReceive
  }), name = ActorMapName)

  val count = AsyncCache.single(
    f = roundMap ? lila.hub.actorApi.map.Size mapTo manifest[Int],
    timeToLive = 1 second)

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
          case m: lila.hub.actorApi.game.ChangeFeatured =>
            self ! lila.hub.actorApi.map.TellAll(actorApi.ChangeFeaturedMsg(
              lila.socket.Socket.makeMessage(
                "featured",
                play.api.libs.json.Json.obj("html" -> m.html.toString))))
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
    bus = system.lilaBus)

  lazy val perfsUpdater = new PerfsUpdater(historyApi)

  private lazy val finisher = new Finisher(
    messenger = messenger,
    perfsUpdater = perfsUpdater,
    aiPerfApi = aiPerfApi,
    crosstableApi = crosstableApi,
    reminder = reminder,
    bus = system.lilaBus,
    casualOnly = CasualOnly)

  private lazy val rematcher = new Rematcher(
    messenger = messenger,
    onStart = onStart,
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
    prefApi = prefApi,
    messenger = messenger,
    finisher = finisher)

  private lazy val cheatDetector = new CheatDetector(reporter = hub.actor.report)

  lazy val cli = new Cli(db, roundMap = roundMap, system = system)

  lazy val messenger = new Messenger(
    socketHub = socketHub,
    chat = hub.actor.chat,
    i18nKeys = i18nKeys)

  def version(gameId: String): Fu[Int] =
    socketHub ? Ask(gameId, GetVersion) mapTo manifest[Int]

  private def getSocketStatus(gameId: String): Fu[SocketStatus] =
    socketHub ? Ask(gameId, GetSocketStatus) mapTo manifest[SocketStatus]

  private lazy val reminder = new Reminder(db(CollectionReminder))
  def nowPlaying = reminder.nowPlaying

  lazy val jsonView = new JsonView(
    chatApi = chatApi,
    userJsonView = userJsonView,
    getSocketStatus = getSocketStatus,
    canTakeback = takebacker.isAllowedByPrefs,
    baseAnimationDuration = AnimationDuration,
    moretimeSeconds = Moretime.toSeconds.toInt)

  {
    import scala.concurrent.duration._

    scheduler.future(0.23 hour, "game: finish by clock") {
      titivate.finishByClock
    }

    scheduler.effect(0.41 hour, "game: finish abandoned") {
      titivate.finishAbandoned
    }

    scheduler.message(1.3 seconds)(roundMap -> actorApi.BroadcastSize)
  }

  private lazy val titivate = new Titivate(roundMap, scheduler)

  lazy val takebacker = new Takebacker(
    messenger = messenger,
    uciMemo = uciMemo,
    prefApi = prefApi)

  lazy val tvBroadcast = system.actorOf(Props(classOf[TvBroadcast]))
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
    userJsonView = lila.user.Env.current.jsonView,
    uciMemo = lila.game.Env.current.uciMemo,
    rematch960Cache = lila.game.Env.current.cached.rematch960,
    onStart = lila.game.Env.current.onStart,
    i18nKeys = lila.i18n.Env.current.keys,
    prefApi = lila.pref.Env.current.api,
    chatApi = lila.chat.Env.current.api,
    historyApi = lila.history.Env.current.api,
    scheduler = lila.common.PlayApp.scheduler)
}
