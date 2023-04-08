package lila.game

import akka.actor.*
import com.softwaremill.macwire.*
import com.softwaremill.tagging.*
import lila.common.autoconfig.{ *, given }
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient

import lila.common.config.*
import akka.stream.Materializer

final private class GameConfig(
    @ConfigName("collection.game") val gameColl: CollName,
    @ConfigName("collection.crosstable") val crosstableColl: CollName,
    @ConfigName("collection.matchup") val matchupColl: CollName,
    @ConfigName("captcher.name") val captcherName: String,
    @ConfigName("captcher.duration") val captcherDuration: FiniteDuration,
    val gifUrl: String
)

@Module
@annotation.nowarn("msg=unused")
final class Env(
    appConfig: Configuration,
    ws: StandaloneWSClient,
    db: lila.db.Db,
    yoloDb: lila.db.AsyncDb @@ lila.db.YoloDb,
    baseUrl: BaseUrl,
    userRepo: lila.user.UserRepo,
    mongoCache: lila.memo.MongoCache.Api,
    lightUserApi: lila.user.LightUserApi,
    cacheApi: lila.memo.CacheApi
)(using
    ec: Executor,
    system: ActorSystem,
    scheduler: Scheduler,
    materializer: Materializer,
    mode: play.api.Mode
):

  private val config = appConfig.get[GameConfig]("game")(AutoConfig.loader)

  lazy val gameRepo = new GameRepo(db(config.gameColl))

  lazy val idGenerator = wire[IdGenerator]

  lazy val gifExport = new GifExport(ws, lightUserApi, baseUrl, config.gifUrl)

  lazy val divider = wire[Divider]

  lazy val cached: Cached = wire[Cached]

  lazy val paginator = wire[PaginatorBuilder]

  lazy val uciMemo = wire[UciMemo]

  lazy val pgnDump = wire[PgnDump]

  lazy val crosstableApi = new CrosstableApi(
    coll = db(config.crosstableColl),
    matchupColl = yoloDb(config.matchupColl).failingSilently()
  )

  lazy val gamesByUsersStream = wire[GamesByUsersStream]
  lazy val gamesByIdsStream   = wire[GamesByIdsStream]

  lazy val favoriteOpponents = wire[FavoriteOpponents]

  lazy val rematches = wire[Rematches]

  lazy val jsonView = wire[JsonView]

  // eagerly load captcher actor
  private val captcher = system.actorOf(Props(new Captcher(gameRepo)), name = config.captcherName)
  scheduler.scheduleWithFixedDelay(config.captcherDuration, config.captcherDuration) { () =>
    captcher ! actorApi.NewCaptcha
  }
