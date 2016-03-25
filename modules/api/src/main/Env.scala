package lila.api

import akka.actor._
import com.typesafe.config.Config
import lila.common.PimpedConfig._
import lila.simul.Simul
import scala.collection.JavaConversions._
import scala.concurrent.duration._

final class Env(
    config: Config,
    db: lila.db.Env,
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
    gamePgnDump: lila.game.PgnDump,
    userEnv: lila.user.Env,
    analyseEnv: lila.analyse.Env,
    lobbyEnv: lila.lobby.Env,
    setupEnv: lila.setup.Env,
    getSimul: Simul.ID => Fu[Option[Simul]],
    getSimulName: Simul.ID => Option[String],
    getTournamentName: String => Option[String],
    val isProd: Boolean) {

  val CliUsername = config getString "cli.username"

  private[api] val apiToken = config getString "api.token"

  object Net {
    val Domain = config getString "net.domain"
    val Protocol = config getString "net.protocol"
    val BaseUrl = config getString "net.base_url"
    val Port = config getInt "http.port"
    val AssetDomain = config getString "net.asset.domain"
    val AssetVersion = config getInt "net.asset.version"
  }
  val PrismicApiUrl = config getString "prismic.api_url"
  val EditorAnimationDuration = config duration "editor.animation.duration"
  val ExplorerEndpoint = config getString "explorer.endpoint"

  object assetVersion {
    import reactivemongo.bson._
    private val coll = db("flag")
    private val cache = lila.memo.MixedCache.single[Int](
      f = coll.find(BSONDocument("_id" -> "asset")).one[BSONDocument].map {
        _.flatMap(_.getAs[BSONNumberLike]("version"))
          .fold(Net.AssetVersion)(_.toInt max Net.AssetVersion)
      },
      timeToLive = 30.seconds,
      default = Net.AssetVersion,
      logger = lila.log("assetVersion"))
    def get = cache get true
  }

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
    simulName = getSimulName,
    tournamentName = getTournamentName)

  val userApi = new UserApi(
    jsonView = userEnv.jsonView,
    makeUrl = makeUrl,
    relationApi = relationApi,
    bookmarkApi = bookmarkApi,
    crosstableApi = crosstableApi,
    prefApi = prefApi)

  val analysisApi = new AnalysisApi

  val gameApi = new GameApi(
    netBaseUrl = Net.BaseUrl,
    apiToken = apiToken,
    pgnDump = pgnDump,
    analysisApi = analysisApi)

  val userGameApi = new UserGameApi(
    bookmarkApi = bookmarkApi)

  val roundApi = new RoundApiBalancer(
    api = new RoundApi(
      jsonView = roundJsonView,
      noteApi = noteApi,
      forecastApi = forecastApi,
      analysisApi = analysisApi,
      bookmarkApi = bookmarkApi,
      getTourAndRanks = getTourAndRanks,
      getSimul = getSimul,
      lightUser = userEnv.lightUser),
    system = system,
    nbActors = math.max(1, math.min(16, Runtime.getRuntime.availableProcessors - 1)))

  val lobbyApi = new LobbyApi(
    lobby = lobbyEnv.lobby,
    lobbyVersion = () => lobbyEnv.history.version,
    getFilter = setupEnv.filter,
    lightUser = userEnv.lightUser,
    seekApi = lobbyEnv.seekApi)

  private def makeUrl(path: String): String = s"${Net.BaseUrl}/$path"

  lazy val cli = new Cli(system.lilaBus, renderer)

  system.actorOf(Props(new KamonPusher(
    countUsers = () => userEnv.onlineUserIdMemo.count
  )))
}

object Env {

  lazy val current = "api" boot new Env(
    config = lila.common.PlayApp.loadConfig,
    db = lila.db.Env.current,
    renderer = lila.hub.Env.current.actor.renderer,
    userEnv = lila.user.Env.current,
    analyseEnv = lila.analyse.Env.current,
    lobbyEnv = lila.lobby.Env.current,
    setupEnv = lila.setup.Env.current,
    getSimul = lila.simul.Env.current.repo.find,
    getSimulName = lila.simul.Env.current.cached.name,
    getTournamentName = lila.tournament.Env.current.cached.name,
    roundJsonView = lila.round.Env.current.jsonView,
    noteApi = lila.round.Env.current.noteApi,
    forecastApi = lila.round.Env.current.forecastApi,
    relationApi = lila.relation.Env.current.api,
    bookmarkApi = lila.bookmark.Env.current.api,
    getTourAndRanks = lila.tournament.Env.current.tourAndRanks,
    crosstableApi = lila.game.Env.current.crosstableApi,
    prefApi = lila.pref.Env.current.api,
    gamePgnDump = lila.game.Env.current.pgnDump,
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler,
    isProd = lila.common.PlayApp.isProd)
}
