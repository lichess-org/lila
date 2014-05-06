package lila.api

import akka.actor._
import com.typesafe.config.Config
import scala.collection.JavaConversions._

final class Env(
    config: Config,
    renderer: akka.actor.ActorSelection,
    router: akka.actor.ActorSelection,
    bus: lila.common.Bus,
    pgnDump: lila.game.PgnDump,
    userEnv: lila.user.Env,
    analyseEnv: lila.analyse.Env,
    puzzleEnv: lila.puzzle.Env,
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

  val userApi = new UserApi(
    makeUrl = apiUrl,
    apiToken = apiToken,
    userIdsSharingIp = userIdsSharingIp,
    isOnline = userEnv.isOnline)

  val gameApi = new GameApi(
    makeUrl = apiUrl,
    apiToken = apiToken,
    isOnline = userEnv.isOnline)

  val analysisApi = new AnalysisApi(
    apiToken = apiToken,
    makeUrl = apiUrl,
    nbAnalysis = () => analyseEnv.cached.nbAnalysis,
    pgnDump = pgnDump)

  val puzzleApi = new PuzzleApi(
    env = puzzleEnv,
    makeUrl = apiUrl)

  private def apiUrl(msg: Any): Fu[String] = {
    import akka.pattern.ask
    import makeTimeout.short
    router ? lila.hub.actorApi.router.Abs(msg) mapTo manifest[String]
  }

  lazy val cli = new Cli(bus, renderer)
}

object Env {

  lazy val current = "[boot] api" describes new Env(
    config = lila.common.PlayApp.loadConfig,
    renderer = lila.hub.Env.current.actor.renderer,
    router = lila.hub.Env.current.actor.router,
    userEnv = lila.user.Env.current,
    analyseEnv = lila.analyse.Env.current,
    puzzleEnv = lila.puzzle.Env.current,
    pgnDump = lila.game.Env.current.pgnDump,
    userIdsSharingIp = lila.security.Env.current.api.userIdsSharingIp,
    bus = lila.common.PlayApp.system.lilaBus,
    isProd = lila.common.PlayApp.isProd)
}
