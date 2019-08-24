package lidraughts.game

import akka.actor._
import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import com.typesafe.config.Config
import scala.concurrent.duration._

final class Env(
    config: Config,
    db: lidraughts.db.Env,
    mongoCache: lidraughts.memo.MongoCache.Builder,
    system: ActorSystem,
    hub: lidraughts.hub.Env,
    getLightUser: lidraughts.common.LightUser.Getter,
    appPath: String,
    isProd: Boolean,
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    settingStore: lidraughts.memo.SettingStore.Builder,
    scheduler: lidraughts.common.Scheduler
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

  lazy val pdnEncodingSetting = settingStore[String](
    "pdnEncodingSetting",
    default = "none",
    text = "Use Huffman encoding for game PDN [none|beta|all]".some
  )

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
    maxPerPage = lidraughts.common.MaxPerPage(PaginatorMaxPerPage)
  )

  lazy val rewind = Rewind

  lazy val uciMemo = new UciMemo(UciMemoTtl)

  lazy val pdnDump = new PdnDump(
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

  val recentGoodGameActor = system.actorOf(Props[RecentGoodGame], name = "recent-good-game")
  system.lidraughtsBus.subscribe(recentGoodGameActor, 'finishGame)

  scheduler.message(CaptcherDuration) {
    captcher -> actorApi.NewCaptcha
  }

  lazy val gamesByUsersStream = new GamesByUsersStream(system)

  lazy val bestOpponents = new BestOpponents

  def cli = new lidraughts.common.Cli {
    def process = {
      case "game" :: "test" :: times :: Nil => parseIntOption(times) ?? StreamTest.start
    }
  }

  lazy val rematches: Cache[Game.ID, Game.ID] = Scaffeine()
    .expireAfterWrite(3 hour)
    .build[Game.ID, Game.ID]

  lazy val jsonView = new JsonView(
    rematchOf = rematches.getIfPresent
  )
}

object Env {

  lazy val current = "game" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "game",
    db = lidraughts.db.Env.current,
    mongoCache = lidraughts.memo.Env.current.mongoCache,
    system = lidraughts.common.PlayApp.system,
    hub = lidraughts.hub.Env.current,
    getLightUser = lidraughts.user.Env.current.lightUser,
    appPath = play.api.Play.current.path.getCanonicalPath,
    isProd = lidraughts.common.PlayApp.isProd,
    asyncCache = lidraughts.memo.Env.current.asyncCache,
    settingStore = lidraughts.memo.Env.current.settingStore,
    scheduler = lidraughts.common.PlayApp.scheduler
  )
}
