package lila.game

import com.typesafe.config.Config
import lila.common.PimpedConfig._
import akka.actor._
import akka.pattern.pipe

final class Env(
    config: Config,
    db: lila.db.Env,
    system: ActorSystem,
    hub: lila.hub.Env,
    appPath: String,
    isProd: Boolean,
    scheduler: lila.common.Scheduler) {

  private val settings = new {
    val CachedNbTtl = config duration "cached.nb.ttl"
    val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
    val CaptcherName = config getString "captcher.name"
    val CollectionGame = config getString "collection.game"
    val CollectionPgn = config getString "collection.pgn"
    val JsPathRaw = config getString "js_path.raw"
    val JsPathCompiled = config getString "js_path.compiled"
    val ActorName = config getString "actor.name"
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

  lazy val export = new Export(hub.actor.router).apply _

  lazy val listMenu = ListMenu(cached) _

  lazy val rewind = Rewind

  lazy val gameJs = new GameJs(path = jsPath, useCache = isProd)

  // load captcher actor
  system.actorOf(Props(new Captcher), name = CaptcherName)

  // api actor
  system.actorOf(Props(new Actor {
    def receive = {
      case lila.hub.actorApi.game.Count ⇒ cached.nbGames pipeTo sender
    }
  }), name = ActorName)

  {
    import scala.concurrent.duration._

    scheduler.effect(4.5 hours, "game: cleanup") {
      titivate.cleanupUnplayed >> titivate.cleanupNext
    }

    scheduler.effect(5.seconds, "") { featured.one }
  }

  def cli = new Cli

  private lazy val titivate = new Titivate(
    bookmark = hub.actor.bookmark)

  private def jsPath =
    "%s/%s".format(appPath, isProd.fold(JsPathCompiled, JsPathRaw))
}

object Env {

  private def app = play.api.Play.current

  lazy val current = "[boot] game" describes new Env(
    config = lila.common.PlayApp loadConfig "game",
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system,
    hub = lila.hub.Env.current,
    appPath = app.path.getCanonicalPath,
    isProd = lila.common.PlayApp.isProd,
    scheduler = lila.common.PlayApp.scheduler
  )
}
