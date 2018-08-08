package lidraughts.api

import akka.actor._
import com.typesafe.config.Config

import lidraughts.simul.Simul

final class Env(
    config: Config,
    settingStore: lidraughts.memo.SettingStore.Builder,
    renderer: ActorSelection,
    system: ActorSystem,
    scheduler: lidraughts.common.Scheduler,
    roundJsonView: lidraughts.round.JsonView,
    noteApi: lidraughts.round.NoteApi,
    forecastApi: lidraughts.round.ForecastApi,
    relationApi: lidraughts.relation.RelationApi,
    bookmarkApi: lidraughts.bookmark.BookmarkApi,
    getTourAndRanks: lidraughts.game.Game => Fu[Option[lidraughts.tournament.TourAndRanks]],
    crosstableApi: lidraughts.game.CrosstableApi,
    prefApi: lidraughts.pref.PrefApi,
    playBanApi: lidraughts.playban.PlaybanApi,
    gamePdnDump: lidraughts.game.PdnDump,
    gameCache: lidraughts.game.Cached,
    userEnv: lidraughts.user.Env,
    analyseEnv: lidraughts.analyse.Env,
    lobbyEnv: lidraughts.lobby.Env,
    setupEnv: lidraughts.setup.Env,
    getSimul: Simul.ID => Fu[Option[Simul]],
    getSimulName: Simul.ID => Fu[Option[String]],
    getTournamentName: String => Option[String],
    pools: List[lidraughts.pool.PoolConfig],
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

  object Accessibility {
    val blindCookieName = config getString "accessibility.blind.cookie.name"
    val blindCookieMaxAge = config getInt "accessibility.blind.cookie.max_age"
    private val blindCookieSalt = config getString "accessibility.blind.cookie.salt"
    def hash(implicit ctx: lidraughts.user.UserContext) = {
      import com.roundeights.hasher.Implicits._
      (ctx.userId | "anon").salt(blindCookieSalt).md5.hex
    }
  }

  val pdnDump = new PdnDump(
    dumper = gamePdnDump,
    getSimulName = getSimulName,
    getTournamentName = getTournamentName
  )

  val userApi = new UserApi(
    jsonView = userEnv.jsonView,
    makeUrl = makeUrl,
    relationApi = relationApi,
    bookmarkApi = bookmarkApi,
    crosstableApi = crosstableApi,
    playBanApi = playBanApi,
    gameCache = gameCache,
    prefApi = prefApi
  )

  val gameApi = new GameApi(
    netBaseUrl = Net.BaseUrl,
    apiToken = apiToken,
    pdnDump = pdnDump,
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

  lazy val cli = new Cli(system.lidraughtsBus)

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
    config = lidraughts.common.PlayApp.loadConfig,
    settingStore = lidraughts.memo.Env.current.settingStore,
    renderer = lidraughts.hub.Env.current.actor.renderer,
    userEnv = lidraughts.user.Env.current,
    analyseEnv = lidraughts.analyse.Env.current,
    lobbyEnv = lidraughts.lobby.Env.current,
    setupEnv = lidraughts.setup.Env.current,
    getSimul = lidraughts.simul.Env.current.repo.find,
    getSimulName = lidraughts.simul.Env.current.api.idToName,
    getTournamentName = lidraughts.tournament.Env.current.cached.name,
    roundJsonView = lidraughts.round.Env.current.jsonView,
    noteApi = lidraughts.round.Env.current.noteApi,
    forecastApi = lidraughts.round.Env.current.forecastApi,
    relationApi = lidraughts.relation.Env.current.api,
    bookmarkApi = lidraughts.bookmark.Env.current.api,
    getTourAndRanks = lidraughts.tournament.Env.current.tourAndRanks,
    crosstableApi = lidraughts.game.Env.current.crosstableApi,
    playBanApi = lidraughts.playban.Env.current.api,
    prefApi = lidraughts.pref.Env.current.api,
    gamePdnDump = lidraughts.game.Env.current.pdnDump,
    gameCache = lidraughts.game.Env.current.cached,
    system = lidraughts.common.PlayApp.system,
    scheduler = lidraughts.common.PlayApp.scheduler,
    pools = lidraughts.pool.Env.current.api.configs,
    isProd = lidraughts.common.PlayApp.isProd
  )
}
