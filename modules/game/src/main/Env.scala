package lila.game

import akka.actor._
import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import com.typesafe.config.Config
import scala.concurrent.duration._

final class Env(
    config: Config,
    db: lila.db.Env,
    mongoCache: lila.memo.MongoCache.Builder,
    system: ActorSystem,
    hub: lila.hub.Env,
    getLightUser: lila.common.LightUser.Getter,
    appPath: String,
    isProd: Boolean,
    asyncCache: lila.memo.AsyncCache.Builder,
    settingStore: lila.memo.SettingStore.Builder,
    scheduler: lila.common.Scheduler
) {

  private val settings = new {
    val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
    val CaptcherName = config getString "captcher.name"
    val CaptcherDuration = config duration "captcher.duration"
    val CollectionGame = config getString "collection.game"
    val CollectionCrosstable = config getString "collection.crosstable"
    val CollectionMatchup = config getString "collection.matchup"
    val UciMemoTtl = config duration "uci_memo.ttl"
    val netBaseUrl = config getString "net.base_url"
    val PngUrl = config getString "png.url"
    val PngSize = config getInt "png.size"
  }
  import settings._

  lazy val gameColl = db(CollectionGame)

  lazy val pngExport = new PngExport(PngUrl, PngSize)

  lazy val divider = new Divider

  lazy val cached = new Cached(
    coll = gameColl,
    asyncCache = asyncCache,
    mongoCache = mongoCache
  )

  lazy val paginator = new PaginatorBuilder(
    coll = gameColl,
    cached = cached,
    maxPerPage = lila.common.MaxPerPage(PaginatorMaxPerPage)
  )

  lazy val rewind = Rewind

  lazy val uciMemo = new UciMemo(UciMemoTtl)

  lazy val pgnDump = new PgnDump(
    netBaseUrl = netBaseUrl,
    getLightUser = getLightUser
  )

  lazy val crosstableApi = new CrosstableApi(
    coll = db(CollectionCrosstable),
    matchupColl = db(CollectionMatchup),
    gameColl = gameColl,
    asyncCache = asyncCache,
    system = system
  )

  lazy val playTime = new PlayTimeApi(gameColl, asyncCache, system)

  // load captcher actor
  private val captcher = system.actorOf(Props(new Captcher), name = CaptcherName)

  scheduler.message(CaptcherDuration) {
    captcher -> actorApi.NewCaptcha
  }

  lazy val gamesByUsersStream = new GamesByUsersStream

  lazy val bestOpponents = new BestOpponents

  lazy val rematches: Cache[Game.ID, Game.ID] = Scaffeine()
    .expireAfterWrite(3 hour)
    .build[Game.ID, Game.ID]

  lazy val jsonView = new JsonView(
    rematchOf = rematches.getIfPresent
  )
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
    asyncCache = lila.memo.Env.current.asyncCache,
    settingStore = lila.memo.Env.current.settingStore,
    scheduler = lila.common.PlayApp.scheduler
  )
}
