package lila.game

import akka.actor._
import akka.pattern.pipe
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    system: ActorSystem,
    hub: lila.hub.Env,
    getLightUser: String => Option[lila.common.LightUser],
    appPath: String,
    isProd: Boolean,
    scheduler: lila.common.Scheduler) {

  private val settings = new {
    val CachedNbTtl = config duration "cached.nb.ttl"
    val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
    val CaptcherName = config getString "captcher.name"
    val CaptcherDuration = config duration "captcher.duration"
    val CollectionGame = config getString "collection.game"
    val CollectionCrosstable = config getString "collection.crosstable"
    val JsPathRaw = config getString "js_path.raw"
    val JsPathCompiled = config getString "js_path.compiled"
    val ActorName = config getString "actor.name"
    val UciMemoTtl = config duration "uci_memo.ttl"
    val netBaseUrl = config getString "net.base_url"
    val PdfExecPath = config getString "pdf.exec_path"
    val PngExecPath = config getString "png.exec_path"
  }
  import settings._

  val MandatorySecondsToMove = config getInt "mandatory.seconds_to_move"

  private[game] lazy val gameColl = db(CollectionGame)

  lazy val pdfExport = PdfExport(PdfExecPath) _

  lazy val pngExport = PngExport(PngExecPath) _

  lazy val cached = new Cached(ttl = CachedNbTtl)

  lazy val paginator = new PaginatorBuilder(
    cached = cached,
    maxPerPage = PaginatorMaxPerPage)

  lazy val export = new PgnExport(pgnDump).apply _

  lazy val listMenu = ListMenu(cached) _

  lazy val rewind = Rewind

  lazy val gameJs = new GameJs(path = jsPath, useCache = isProd)

  lazy val uciMemo = new UciMemo(UciMemoTtl)

  lazy val pgnDump = new PgnDump(
    netBaseUrl = netBaseUrl,
    getLightUser = getLightUser)

  lazy val crosstableApi = new CrosstableApi(db(CollectionCrosstable))

  // load captcher actor
  private val captcher = system.actorOf(Props(new Captcher), name = CaptcherName)

  // api actor
  system.actorOf(Props(new Actor {
    def receive = {
      case lila.hub.actorApi.game.Count => cached.nbGames pipeTo sender
    }
  }), name = ActorName)

  {
    import scala.concurrent.duration._

    scheduler.effect(0.9 hours, "game: cleanup") {
      maintenance.cleanupUnplayed
    }

    scheduler.message(CaptcherDuration) {
      captcher -> actorApi.NewCaptcha
    }
  }

  def cli = new Cli(db, system = system)

  def onStart(gameId: String) = GameRepo game gameId foreach {
    _ foreach { game =>
      // nobody needs it for now
      // system.lilaBus.publish(actorApi.StartGame(game), 'startGame)
      game.userIds foreach { userId =>
        system.lilaBus.publish(
          actorApi.UserStartGame(userId, game),
          Symbol(s"userStartGame:$userId"))
      }
    }
  }

  lazy val maintenance = new Maintenance(scheduler, hub.actor.bookmark)

  private def jsPath =
    "%s/%s".format(appPath, isProd.fold(JsPathCompiled, JsPathRaw))
}

object Env {

  lazy val current = "[boot] game" describes new Env(
    config = lila.common.PlayApp loadConfig "game",
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system,
    hub = lila.hub.Env.current,
    getLightUser = lila.user.Env.current.lightUser,
    appPath = play.api.Play.current.path.getCanonicalPath,
    isProd = lila.common.PlayApp.isProd,
    scheduler = lila.common.PlayApp.scheduler)
}
