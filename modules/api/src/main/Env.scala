package lila.api

import akka.actor._
import com.typesafe.config.Config

import lila.simul.Simul

final class Env(
    config: Config,
    settingStore: lila.memo.SettingStore.Builder,
    renderer: ActorSelection,
    system: ActorSystem,
    scheduler: lila.common.Scheduler,
    roundJsonView: lila.round.JsonView,
    noteApi: lila.round.NoteApi,
    forecastApi: lila.round.ForecastApi,
    relationApi: lila.relation.RelationApi,
    bookmarkApi: lila.bookmark.BookmarkApi,
    getTourAndRanks: lila.game.Game => Fu[Option[lila.tournament.TourAndRanks]],
    crosstableApi: lila.game.CrosstableApi,
    prefApi: lila.pref.PrefApi,
    playBanApi: lila.playban.PlaybanApi,
    gamePgnDump: lila.game.PgnDump,
    gameCache: lila.game.Cached,
    userEnv: lila.user.Env,
    annotator: lila.analyse.Annotator,
    lobbyEnv: lila.lobby.Env,
    setupEnv: lila.setup.Env,
    getSimul: Simul.ID => Fu[Option[Simul]],
    getSimulName: Simul.ID => Fu[Option[String]],
    getTournamentName: String => Option[String],
    isStreaming: lila.user.User.ID => Boolean,
    isPlaying: lila.user.User.ID => Boolean,
    pools: List[lila.pool.PoolConfig],
    challengeJsonView: lila.challenge.JsonView,
    val isProd: Boolean
) {

  val apiToken = config getString "api.token"

  val isStage = config getBoolean "app.stage"

  object Net {
    val Domain = config getString "net.domain"
    val Protocol = config getString "net.protocol"
    val BaseUrl = config getString "net.base_url"
    val Port = config getInt "http.port"
    val AssetDomain = config getString "net.asset.domain"
    val SocketDomain = config getString "net.socket.domain"
    val RemoteSocketDomain = config getString "net.socket.remote.domain"
    val Email = config getString "net.email"
    val Crawlable = config getBoolean "net.crawlable"
    val RateLimit = config getBoolean "net.ratelimit"
  }
  val PrismicApiUrl = config getString "prismic.api_url"
  val EditorAnimationDuration = config duration "editor.animation.duration"
  val ExplorerEndpoint = config getString "explorer.endpoint"
  val TablebaseEndpoint = config getString "explorer.tablebase.endpoint"

  private val InfluxEventEndpoint = config getString "api.influx_event.endpoint"
  private val InfluxEventEnv = config getString "api.influx_event.env"

  val cspEnabledSetting = settingStore[Boolean](
    "cspEnabled",
    default = true,
    text = "Enable CSP for everyone.".some
  )

  import lila.memo.SettingStore.Regex._
  import lila.memo.SettingStore.Formable.regexFormable
  val serviceWorkerSetting = settingStore[scala.util.matching.Regex](
    "serviceWorkerUsers",
    default = "".r,
    text = "Regex selecting user IDs using service workers".some
  )

  object Accessibility {
    val blindCookieName = config getString "accessibility.blind.cookie.name"
    val blindCookieMaxAge = config getInt "accessibility.blind.cookie.max_age"
    private val blindCookieSalt = config getString "accessibility.blind.cookie.salt"
    def hash(implicit ctx: lila.user.UserContext) = {
      import com.roundeights.hasher.Implicits._
      (ctx.userId | "anon").salt(blindCookieSalt).md5.hex
    }
  }

  val pgnDump = new PgnDump(
    dumper = gamePgnDump,
    annotator = annotator,
    getSimulName = getSimulName,
    getTournamentName = getTournamentName
  )

  val userApi = new UserApi(
    jsonView = userEnv.jsonView,
    lightUserApi = userEnv.lightUserApi,
    makeUrl = makeUrl,
    relationApi = relationApi,
    bookmarkApi = bookmarkApi,
    crosstableApi = crosstableApi,
    playBanApi = playBanApi,
    gameCache = gameCache,
    isStreaming = isStreaming,
    isPlaying = isPlaying,
    isOnline = userEnv.onlineUserIdMemo.get,
    recentTitledUserIds = () => userEnv.recentTitledUserIdMemo.keys,
    prefApi = prefApi
  )(system)

  val gameApi = new GameApi(
    netBaseUrl = Net.BaseUrl,
    apiToken = apiToken,
    pgnDump = pgnDump,
    gameCache = gameCache,
    crosstableApi = crosstableApi
  )

  val gameApiV2 = new GameApiV2(
    pgnDump = pgnDump,
    getLightUser = userEnv.lightUser
  )(system)

  val userGameApi = new UserGameApi(
    bookmarkApi = bookmarkApi,
    lightUser = userEnv.lightUserSync,
    getTournamentName = getTournamentName
  )

  val roundApi = new RoundApi(
    jsonView = roundJsonView,
    noteApi = noteApi,
    forecastApi = forecastApi,
    bookmarkApi = bookmarkApi,
    getTourAndRanks = getTourAndRanks,
    getSimul = getSimul
  )

  val lobbyApi = new LobbyApi(
    getFilter = setupEnv.filter,
    lightUserApi = userEnv.lightUserApi,
    seekApi = lobbyEnv.seekApi,
    pools = pools
  )

  lazy val eventStream = new EventStream(system, challengeJsonView, userEnv.onlineUserIdMemo.put)

  private def makeUrl(path: String): String = s"${Net.BaseUrl}/$path"

  lazy val cli = new Cli(system.lilaBus)

  KamonPusher.start(system) {
    new KamonPusher(countUsers = () => userEnv.onlineUserIdMemo.count)
  }

  if (InfluxEventEnv != "dev") system.actorOf(Props(new InfluxEvent(
    endpoint = InfluxEventEndpoint,
    env = InfluxEventEnv
  )), name = "influx-event")

  system.registerOnTermination {
    system.lilaBus.publish(lila.hub.actorApi.Shutdown, 'shutdown)
  }
}

object Env {

  lazy val current = "api" boot new Env(
    config = lila.common.PlayApp.loadConfig,
    settingStore = lila.memo.Env.current.settingStore,
    renderer = lila.hub.Env.current.renderer,
    userEnv = lila.user.Env.current,
    annotator = lila.analyse.Env.current.annotator,
    lobbyEnv = lila.lobby.Env.current,
    setupEnv = lila.setup.Env.current,
    getSimul = lila.simul.Env.current.repo.find,
    getSimulName = lila.simul.Env.current.api.idToName,
    getTournamentName = lila.tournament.Env.current.cached.name,
    roundJsonView = lila.round.Env.current.jsonView,
    noteApi = lila.round.Env.current.noteApi,
    forecastApi = lila.round.Env.current.forecastApi,
    relationApi = lila.relation.Env.current.api,
    bookmarkApi = lila.bookmark.Env.current.api,
    getTourAndRanks = lila.tournament.Env.current.tourAndRanks,
    crosstableApi = lila.game.Env.current.crosstableApi,
    playBanApi = lila.playban.Env.current.api,
    prefApi = lila.pref.Env.current.api,
    gamePgnDump = lila.game.Env.current.pgnDump,
    gameCache = lila.game.Env.current.cached,
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler,
    isStreaming = lila.streamer.Env.current.liveStreamApi.isStreaming,
    isPlaying = lila.relation.Env.current.online.isPlaying,
    pools = lila.pool.Env.current.api.configs,
    challengeJsonView = lila.challenge.Env.current.jsonView,
    isProd = lila.common.PlayApp.isProd
  )
}
