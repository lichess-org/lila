package lila.game

import com.typesafe.config.Config
import lila.common.PimpedConfig._
import akka.actor._

final class Env(
    config: Config,
    db: lila.db.Env,
    system: ActorSystem,
    hub: lila.hub.Env,
    appPath: String,
    isDev: Boolean) {

  val CachedNbTtl = config duration "cached.nb.ttl"
  val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
  val CollectionGame = config getString "collection.game"
  val CollectionPgn = config getString "collection.pgn"
  val JsPathRaw = config getString "js_path.raw"
  val JsPathCompiled = config getString "js_path.compiled"

  lazy val gameRepo = new GameRepo()(db(CollectionGame))

  lazy val pgnRepo = new PgnRepo()(db(CollectionPgn))

  lazy val cached = new Cached(gameRepo = gameRepo, ttl = CachedNbTtl)

  lazy val paginator = new PaginatorBuilder(
    gameRepo = gameRepo,
    cached = cached,
    maxPerPage = PaginatorMaxPerPage)

  lazy val featured = new Featured(
    gameRepo = gameRepo,
    lobbyActor = hub.actor.lobby,
    rendererActor = hub.actor.renderer,
    system = system)

  // lazy val export = Export(gameRepo, NetBaseUrl) _

  lazy val listMenu = ListMenu(cached) _

  lazy val rewind = Rewind

  lazy val gameJs = new GameJs(path = jsPath, useCache = !isDev)

  private def jsPath =
    "%s/%s".format(appPath, isDev.fold(JsPathRaw, JsPathCompiled))
}

object Env {

  private def hub = lila.hub.Env.current
  private def app = play.api.Play.current

  lazy val current = new Env(
    config = lila.common.PlayApp loadConfig "game",
    db = lila.db.Env.current,
    system = play.api.libs.concurrent.Akka.system(app),
    hub = lila.hub.Env.current,
    appPath = app.path.getCanonicalPath,
    isDev = app.mode == play.api.Mode.Dev
  )
}
