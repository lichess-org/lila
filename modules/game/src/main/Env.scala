package lila.game

import akka.actor._
import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import play.api.libs.ws.WSClient
import scala.concurrent.duration._

import lila.common.config._

private case class GameConfig(
    @ConfigName("collection.game") gameColl: CollName,
    @ConfigName("collection.crosstable") crosstableColl: CollName,
    @ConfigName("collection.matchup") matchupColl: CollName,
    @ConfigName("paginator.maxPerPage") paginatorMaxPerPage: MaxPerPage,
    @ConfigName("captcher.name") captcherName: String,
    @ConfigName("captcher.duration") captcherDuration: FiniteDuration,
    uciMemoTtl: FiniteDuration,
    pngUrl: String,
    pngSize: Int
)

final class Env(
    appConfig: Configuration,
    ws: WSClient,
    db: lila.db.Env,
    baseUrl: BaseUrl,
    userRepo: lila.user.UserRepo,
    mongoCache: lila.memo.MongoCache.Builder,
    hub: lila.hub.Env,
    getLightUser: lila.common.LightUser.Getter,
    asyncCache: lila.memo.AsyncCache.Builder,
    settingStore: lila.memo.SettingStore.Builder
)(implicit system: ActorSystem, scheduler: Scheduler) {

  private val config = appConfig.get[GameConfig]("game")(AutoConfig.loader)
  import config._

  lazy val gameRepo = new GameRepo(db(gameColl))

  lazy val pngExport = new PngExport(ws, pngUrl, pngSize)

  lazy val divider = wire[Divider]

  lazy val cached: Cached = wire[Cached]

  lazy val paginator = new PaginatorBuilder(gameRepo, cached, paginatorMaxPerPage)
  // lazy val paginator = wire[PaginatorBuilder]

  lazy val rewind = wire[Rewind]

  lazy val uciMemo = new UciMemo(gameRepo, uciMemoTtl)

  lazy val pgnDump = wire[PgnDump]

  lazy val crosstableApi = new CrosstableApi(
    coll = db(crosstableColl),
    matchupColl = db(matchupColl),
    userRepo = userRepo,
    gameRepo = gameRepo,
    asyncCache = asyncCache
  )

  lazy val playTime = wire[PlayTimeApi]

  lazy val gamesByUsersStream = wire[GamesByUsersStream]

  lazy val bestOpponents = wire[BestOpponents]

  lazy val rematches: Cache[Game.ID, Game.ID] = Scaffeine()
    .expireAfterWrite(3 hour)
    .build[Game.ID, Game.ID]

  lazy val jsonView = new JsonView(rematchOf = rematches.getIfPresent)

  // eargerly load captcher actor
  private val captcher = system.actorOf(Props(new Captcher(gameRepo)), name = captcherName)
  scheduler.scheduleWithFixedDelay(captcherDuration, captcherDuration) {
    () => captcher ! actorApi.NewCaptcha
  }
}
