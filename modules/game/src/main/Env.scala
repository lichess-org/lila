package lila.game

import akka.actor.*
import akka.stream.Materializer
import com.softwaremill.macwire.*
import com.softwaremill.tagging.*
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient

import lila.common.autoconfig.{ *, given }
import lila.core.config.*
import lila.core.game.Game

final private class GameConfig(
    @ConfigName("collection.game") val gameColl: CollName,
    @ConfigName("collection.crosstable") val crosstableColl: CollName,
    @ConfigName("collection.matchup") val matchupColl: CollName,
    @ConfigName("captcher.duration") val captcherDuration: FiniteDuration,
    val gifUrl: String
)

@Module
final class Env(
    appConfig: Configuration,
    ws: StandaloneWSClient,
    db: lila.db.Db,
    yoloDb: lila.db.AsyncDb @@ lila.db.YoloDb,
    baseUrl: BaseUrl,
    userApi: lila.core.user.UserApi,
    mongoCache: lila.memo.MongoCache.Api,
    lightUserApi: lila.core.user.LightUserApi,
    cacheApi: lila.memo.CacheApi,
    getTourName: => lila.core.tournament.GetTourName
)(using system: ActorSystem, scheduler: Scheduler)(using
    lila.core.i18n.Translator,
    Executor,
    Materializer,
    play.api.Mode
):
  private val config = appConfig.get[GameConfig]("game")(AutoConfig.loader)

  val gameRepo = new GameRepo(db(config.gameColl))

  val idGenerator = wire[IdGenerator]

  val divider = wire[Divider]

  val cached: Cached = wire[Cached]

  val uciMemo = wire[UciMemo]

  lazy val gifExport = new GifExport(ws, lightUserApi, baseUrl, config.gifUrl)

  lazy val paginator = wire[PaginatorBuilder]

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

  lazy val captcha = wire[CaptchaApi]

  lazy val importer = wire[lila.game.importer.Importer]

  lazy val userGameApi = UserGameApi(lightUserApi, getTourName)

  lazy val api: lila.core.game.GameApi = new:
    export gameRepo.{ incBookmarks, getSourceAndUserIds }
    export cached.nbPlaying
    export GameExt.{ computeMoveTimes, analysable }
    export AnonCookie.json as anonCookieJson
    export AnonCookie.name as anonCookieName

  lazy val newPlayer: lila.core.game.NewPlayer = new:
    export Player.make as apply
    export Player.makeAnon as anon

  val namer: lila.core.game.Namer = Namer

  scheduler.scheduleWithFixedDelay(config.captcherDuration, config.captcherDuration): () =>
    captcha.newCaptcha()
