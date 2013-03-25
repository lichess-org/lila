package lila.game

import com.typesafe.config.Config
import lila.common.PimpedConfig._
import akka.actor._

final class Env(
  config: Config, 
  db: lila.db.Env,
  system: ActorSystem,
  hub: lila.hub.Env) {

  val CachedNbTtl = config duration "cached.nb.ttl"
  val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
  val CollectionGame = config getString "collection.game"
  val CollectionPgn = config getString "collection.pgn"
  val JsPathRaw = config getString "js_path.raw"
  val JsPathCompiled = config getString "js_path.compiled"

  lazy val gameRepo = new GameRepo(db(CollectionGame))

  // lazy val pgnRepo = new PgnRepo(db(CollectionPgn))

  lazy val cached = new Cached(gameRepo = gameRepo, ttl = CachedNbTtl)

  // lazy val paginator = new PaginatorBuilder(
  //   gameRepo = gameRepo,
  //   cached = cached,
  //   maxPerPage = PaginatorMaxPerPage)

  lazy val featured = new Featured(
    gameRepo = gameRepo,
    lobbyActor = hub.lobbyActor,
    rendererActor = hub.rendererActor,
    system = system)

  // lazy val export = Export(gameRepo, NetBaseUrl) _

  // lazy val listMenu = ListMenu(cached) _

  // lazy val rewind = new Rewind

  // lazy val gameJs = new GameJs(
  //   path = app.path.getCanonicalPath + "/" + IsDev.fold(JsPathRaw, JsPathCompiled),
  //   useCache = !IsDev)
}

object Env {

  lazy val hub = lila.hub.Env.current

  lazy val current = new Env(
    config = lila.common.PlayApp loadConfig "game",
    db = lila.db.Env.current,
    system = play.api.libs.concurrent.Akka.system(play.api.Play.current),
    hub = lila.hub.Env.current)
}
