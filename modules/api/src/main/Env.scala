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
    analyseEnv: lila.analyse.Env,
    lobbyEnv: lila.lobby.Env,
    setupEnv: lila.setup.Env,
    getSimul: Simul.ID => Fu[Option[Simul]],
    getSimulName: Simul.ID => Fu[Option[String]],
    getTournamentName: String => Option[String],
    isStreaming: lila.user.User.ID => Boolean,
    isPlaying: lila.user.User.ID => Boolean,
    pools: List[lila.pool.PoolConfig],
    val isProd: Boolean
) {

  val CliUsername = config getString "cli.username"

  val apiToken = config getString "api.token"

  val isStage = config getBoolean "app.stage"

  object Net {
    val Domain = config getString "net.domain"
    val Protocol = config getString "net.protocol"
    val BaseUrl = config getString "net.base_url"
    val Port = config getInt "http.port"
    val AssetDomain = config getString "net.asset.domain"
    val SocketDomain = config getString "net.socket.domain"
    val Email = config getString "net.email"
    val Crawlable = config getBoolean "net.crawlable"
  }
  val PrismicApiUrl = config getString "prismic.api_url"
  val EditorAnimationDuration = config duration "editor.animation.duration"
  val ExplorerEndpoint = config getString "explorer.endpoint"
  val TablebaseEndpoint = config getString "explorer.tablebase.endpoint"

  private val InfluxEventEndpoint = config getString "api.influx_event.endpoint"
  private val InfluxEventEnv = config getString "api.influx_event.env"

  val assetVersionSetting = settingStore[Int](
    "assetVersion",
    default = config getInt "net.asset.version",
    text = "Assets version. Increment to force all clients to load a new version of static assets. Decrement to serve a previous revision of static assets.".some,
    init = (config, db) => config.value max db.value
  )
  val roundRouterSetting = settingStore[Boolean](
    "roundRouter",
    default = false,
    text = "enable round router".some
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
    getSimulName = getSimulName,
    getTournamentName = getTournamentName
  )(system)

  val userApi = new UserApi(
    jsonView = userEnv.jsonView,
    makeUrl = makeUrl,
    relationApi = relationApi,
    bookmarkApi = bookmarkApi,
    crosstableApi = crosstableApi,
    playBanApi = playBanApi,
    gameCache = gameCache,
    isStreaming = isStreaming,
    isPlaying = isPlaying,
    prefApi = prefApi
  )

  val gameApi = new GameApi(
    netBaseUrl = Net.BaseUrl,
    apiToken = apiToken,
    pgnDump = pgnDump,
    gameCache = gameCache,
    crosstableApi = crosstableApi
  )

  val userGameApi = new UserGameApi(
    bookmarkApi = bookmarkApi,
    lightUser = userEnv.lightUserSync
  )

  val roundApi = new RoundApiBalancer(
    api = new RoundApi(
      jsonView = roundJsonView,
      noteApi = noteApi,
      forecastApi = forecastApi,
      bookmarkApi = bookmarkApi,
      getTourAndRanks = getTourAndRanks,
      getSimul = getSimul
    ),
    enabled = roundRouterSetting.get,
    system = system,
    nbActors = math.max(1, math.min(16, Runtime.getRuntime.availableProcessors - 1))
  )

  val lobbyApi = new LobbyApi(
    getFilter = setupEnv.filter,
    lightUserApi = userEnv.lightUserApi,
    seekApi = lobbyEnv.seekApi,
    pools = pools
  )

  private def makeUrl(path: String): String = s"${Net.BaseUrl}/$path"

  lazy val cli = new Cli(system.lilaBus)

  KamonPusher.start(system) {
    new KamonPusher(countUsers = () => userEnv.onlineUserIdMemo.count)
  }

  if (InfluxEventEnv != "dev") system.actorOf(Props(new InfluxEvent(
    endpoint = InfluxEventEndpoint,
    env = InfluxEventEnv
  )), name = "influx-event")
}

object Env {

  lazy val current = "api" boot new Env(
    config = lila.common.PlayApp.loadConfig,
    settingStore = lila.memo.Env.current.settingStore,
    renderer = lila.hub.Env.current.actor.renderer,
    userEnv = lila.user.Env.current,
    analyseEnv = lila.analyse.Env.current,
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
    isProd = lila.common.PlayApp.isProd
  )
}
