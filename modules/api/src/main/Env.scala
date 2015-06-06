package lila.api

import akka.actor._
import com.typesafe.config.Config
import lila.common.PimpedConfig._
import lila.simul.Simul
import scala.collection.JavaConversions._

final class Env(
    config: Config,
    renderer: ActorSelection,
    router: ActorSelection,
    system: ActorSystem,
    roundJsonView: lila.round.JsonView,
    noteApi: lila.round.NoteApi,
    relationApi: lila.relation.RelationApi,
    bookmarkApi: lila.bookmark.BookmarkApi,
    crosstableApi: lila.game.CrosstableApi,
    prefApi: lila.pref.PrefApi,
    pgnDump: lila.game.PgnDump,
    userEnv: lila.user.Env,
    analyseEnv: lila.analyse.Env,
    lobbyEnv: lila.lobby.Env,
    setupEnv: lila.setup.Env,
    getSimul: Simul.ID => Fu[Option[Simul]],
    userIdsSharingIp: String => Fu[List[String]],
    val isProd: Boolean) {

  val CliUsername = config getString "cli.username"

  private[api] val apiToken = config getString "api.token"

  object Net {
    val Domain = config getString "net.domain"
    val Protocol = config getString "net.protocol"
    val BaseUrl = config getString "net.base_url"
    val Port = config getInt "http.port"
    val ExtraPorts = (config getStringList "net.extra_ports").toList
    val AssetDomain = config getString "net.asset.domain"
    val AssetVersion = config getInt "net.asset.version"
  }
  val PrismicApiUrl = config getString "prismic.api_url"
  val EditorAnimationDuration = config duration "editor.animation.duration"

  object Accessibility {
    val blindCookieName = config getString "accessibility.blind.cookie.name"
    val blindCookieMaxAge = config getInt "accessibility.blind.cookie.max_age"
    private val blindCookieSalt = config getString "accessibility.blind.cookie.salt"
    def hash(implicit ctx: lila.user.UserContext) = {
      import com.roundeights.hasher.Implicits._
      (ctx.userId | "anon").salt(blindCookieSalt).md5.hex
    }
  }

  val userApi = new UserApi(
    jsonView = userEnv.jsonView,
    makeUrl = apiUrl,
    apiToken = apiToken,
    relationApi = relationApi,
    bookmarkApi = bookmarkApi,
    crosstableApi = crosstableApi,
    prefApi = prefApi,
    userIdsSharingIp = userIdsSharingIp)

  val analysisApi = new AnalysisApi

  val gameApi = new GameApi(
    netBaseUrl = Net.BaseUrl,
    apiToken = apiToken,
    pgnDump = pgnDump,
    analysisApi = analysisApi)

  val userGameApi = new UserGameApi(
    bookmarkApi = bookmarkApi)

  val roundApi = new RoundApi(
    jsonView = roundJsonView,
    noteApi = noteApi,
    analysisApi = analysisApi,
    getSimul = getSimul,
    lightUser = userEnv.lightUser)

  val lobbyApi = new LobbyApi(
    lobby = lobbyEnv.lobby,
    lobbyVersion = () => lobbyEnv.history.version,
    getFilter = setupEnv.filter,
    lightUser = userEnv.lightUser,
    seekApi = lobbyEnv.seekApi)

  private def apiUrl(msg: Any): Fu[String] = {
    import akka.pattern.ask
    import makeTimeout.short
    router ? lila.hub.actorApi.router.Abs(msg) mapTo manifest[String]
  }

  lazy val cli = new Cli(system.lilaBus, renderer)
}

object Env {

  lazy val current = "[boot] api" describes new Env(
    config = lila.common.PlayApp.loadConfig,
    renderer = lila.hub.Env.current.actor.renderer,
    router = lila.hub.Env.current.actor.router,
    userEnv = lila.user.Env.current,
    analyseEnv = lila.analyse.Env.current,
    lobbyEnv = lila.lobby.Env.current,
    setupEnv = lila.setup.Env.current,
    getSimul = lila.simul.Env.current.repo.find,
    roundJsonView = lila.round.Env.current.jsonView,
    noteApi = lila.round.Env.current.noteApi,
    relationApi = lila.relation.Env.current.api,
    bookmarkApi = lila.bookmark.Env.current.api,
    crosstableApi = lila.game.Env.current.crosstableApi,
    prefApi = lila.pref.Env.current.api,
    pgnDump = lila.game.Env.current.pgnDump,
    userIdsSharingIp = lila.security.Env.current.api.userIdsSharingIp,
    system = lila.common.PlayApp.system,
    isProd = lila.common.PlayApp.isProd)
}
