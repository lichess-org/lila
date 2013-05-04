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

  private val settings = new {
    val CachedNbTtl = config duration "cached.nb.ttl"
    val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
    val CaptcherName = config getString "captcher.name"
    val CollectionGame = config getString "collection.game"
    val CollectionPgn = config getString "collection.pgn"
    val JsPathRaw = config getString "js_path.raw"
    val JsPathCompiled = config getString "js_path.compiled"
  }
  import settings._

  private[game] lazy val gameColl = db(CollectionGame)

  private[game] lazy val pgnColl = db(CollectionPgn)

  lazy val cached = new Cached(ttl = CachedNbTtl)

  lazy val paginator = new PaginatorBuilder(
    cached = cached,
    maxPerPage = PaginatorMaxPerPage)

  lazy val featured = new Featured(
    lobbySocket = hub.socket.lobby,
    rendererActor = hub.actor.renderer,
    system = system)

  // lazy val export = Export(NetBaseUrl) _

  lazy val listMenu = ListMenu(cached) _

  lazy val rewind = Rewind

  lazy val gameJs = new GameJs(path = jsPath, useCache = !isDev)

  // load captcher actor
  system.actorOf(Props(new Captcher), name = CaptcherName)

  if (!isDev) {

    val scheduler = new lila.common.Scheduler(system)
    import scala.concurrent.duration._

    scheduler.effect(4.5 hours, "game: cleanup") {
      titivate.cleanupUnplayed >> titivate.cleanupNext
    }
  }

  def cli = new lila.common.Cli {
    def process = {
      case "game" :: "per" :: "day" :: days ⇒
        GameRepo nbPerDay {
          (days.headOption flatMap parseIntOption) | 30
        } map (_ mkString " ")
    }
  }

  private lazy val titivate = new Titivate(
    bookmark = hub.actor.bookmark)

  private def jsPath =
    "%s/%s".format(appPath, isDev.fold(JsPathRaw, JsPathCompiled))
}

object Env {

  private def app = play.api.Play.current

  lazy val current = "[boot] game" describes new Env(
    config = lila.common.PlayApp loadConfig "game",
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system,
    hub = lila.hub.Env.current,
    appPath = app.path.getCanonicalPath,
    isDev = lila.common.PlayApp.isDev
  )
}
