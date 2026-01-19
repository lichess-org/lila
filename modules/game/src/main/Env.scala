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
    routeUrl: RouteUrl,
    userApi: lila.core.user.UserApi,
    mongoCache: lila.memo.MongoCache.Api,
    lightUserApi: lila.core.user.LightUserApi,
    cacheApi: lila.memo.CacheApi,
    getTourName: => lila.core.tournament.GetTourName,
    fideIdOf: lila.core.user.PublicFideIdOf
)(using scheduler: Scheduler)(using Executor, Materializer):
  private val config = appConfig.get[GameConfig]("game")(using AutoConfig.loader)

  val gameRepo = GameRepo(db(config.gameColl))

  given idGenerator: IdGenerator = wire[IdGenerator]

  val divider = wire[Divider]

  val cached: Cached = wire[Cached]

  val uciMemo = wire[UciMemo]

  lazy val gifExport = new GifExport(ws, lightUserApi, routeUrl, config.gifUrl)

  lazy val paginator = wire[PaginatorBuilder]

  lazy val pgnDump = wire[PgnDump]

  lazy val crosstableApi = new CrosstableApi(
    coll = db(config.crosstableColl),
    matchupColl = yoloDb(config.matchupColl).failingSilently()
  )

  lazy val gamesByUsersStream = wire[GamesByUsersStream]
  lazy val gamesByIdsStream = wire[GamesByIdsStream]

  lazy val favoriteOpponents = wire[FavoriteOpponents]

  lazy val rematches = wire[Rematches]

  lazy val jsonView = wire[JsonView]

  lazy val captcha = wire[CaptchaApi]

  lazy val importer = wire[lila.game.importer.Importer]

  lazy val userGameApi = UserGameApi(lightUserApi, getTourName)

  lazy val api: lila.core.game.GameApi = new:
    export gameRepo.{ incBookmarks, getSourceAndUserIds }
    override def nbPlaying(userId: UserId): Fu[Int] = cached.nbPlaying(userId)
    export GameExt.{ computeMoveTimes, analysable }
    export AnonCookie.json as anonCookieJson

  given newPlayer: lila.core.game.NewPlayer = new:
    export Player.make as apply
    override def anon(color: Color, aiLevel: Option[Int] = None) =
      Player.makeAnon(color, aiLevel)

  val namer: lila.core.game.Namer = Namer

  scheduler.scheduleWithFixedDelay(config.captcherDuration, config.captcherDuration): () =>
    captcha.newCaptcha()
