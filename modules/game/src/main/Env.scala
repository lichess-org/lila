package lila.game

import akka.actor._
import akka.pattern.pipe
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    mongoCache: lila.memo.MongoCache.Builder,
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
    val UciMemoTtl = config duration "uci_memo.ttl"
    val netBaseUrl = config getString "net.base_url"
    val PdfExecPath = config getString "pdf.exec_path"
    val PngExecPath = config getString "png.exec_path"
  }
  import settings._

  lazy val gameColl = db(CollectionGame)

  lazy val pdfExport = PdfExport(PdfExecPath) _

  lazy val pngExport = PngExport(PngExecPath) _

  lazy val cached = new Cached(
    coll = gameColl,
    mongoCache = mongoCache,
    defaultTtl = CachedNbTtl)

  lazy val paginator = new PaginatorBuilder(
    coll = gameColl,
    cached = cached,
    maxPerPage = PaginatorMaxPerPage)

  lazy val rewind = Rewind

  lazy val gameJs = new GameJs(path = jsPath, useCache = isProd)

  lazy val uciMemo = new UciMemo(UciMemoTtl)

  lazy val pgnDump = new PgnDump(
    netBaseUrl = netBaseUrl,
    getLightUser = getLightUser)

  lazy val crosstableApi = new CrosstableApi(
    coll = db(CollectionCrosstable),
    gameColl = gameColl)

  // load captcher actor
  private val captcher = system.actorOf(Props(new Captcher), name = CaptcherName)

  scheduler.message(CaptcherDuration) {
    captcher -> actorApi.NewCaptcha
  }

  def cli = new Cli(gameColl)

  def onStart(gameId: String) = GameRepo game gameId foreach {
    _ foreach { game =>
      system.lilaBus.publish(actorApi.StartGame(game), 'startGame)
      game.userIds foreach { userId =>
        system.lilaBus.publish(
          actorApi.UserStartGame(userId, game),
          Symbol(s"userStartGame:$userId"))
      }
    }
  }

  private def jsPath =
    "%s/%s".format(appPath, isProd.fold(JsPathCompiled, JsPathRaw))
}

object Env {

  lazy val current = "game" boot new Env(
    config = lila.common.PlayApp loadConfig "game",
    db = lila.db.Env.current,
    mongoCache = lila.memo.Env.current.mongoCache,
    system = lila.common.PlayApp.system,
    hub = lila.hub.Env.current,
    getLightUser = lila.user.Env.current.lightUser,
    appPath = play.api.Play.current.path.getCanonicalPath,
    isProd = lila.common.PlayApp.isProd,
    scheduler = lila.common.PlayApp.scheduler)
}
