package lila.fishnet

import akka.actor.*
import com.softwaremill.macwire.*
import com.softwaremill.tagging.*
import io.lettuce.core.*
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient

import lila.common.Bus
import lila.common.autoconfig.{ *, given }
import lila.core.config.*

@Module
private class FishnetConfig(
    @ConfigName("collection.analysis") val analysisColl: CollName,
    @ConfigName("collection.client") val clientColl: CollName,
    @ConfigName("offline_mode") val offlineMode: Boolean,
    @ConfigName("client_min_version") val clientMinVersion: String,
    @ConfigName("redis.uri") val redisUri: String,
    val explorerEndpoint: String
)

@Module
final class Env(
    appConfig: Configuration,
    uciMemo: lila.core.game.UciMemo,
    requesterApi: lila.analyse.RequesterApi,
    getSinglePvEval: lila.tree.CloudEval.GetSinglePvEval,
    gameRepo: lila.core.game.GameRepo,
    gameApi: lila.core.game.GameApi,
    analysisRepo: lila.analyse.AnalysisRepo,
    userApi: lila.core.user.UserApi,
    db: lila.db.Db,
    cacheApi: lila.memo.CacheApi,
    settingStore: lila.memo.SettingStore.Builder,
    ws: StandaloneWSClient,
    sink: lila.analyse.Analyser,
    shutdown: akka.actor.CoordinatedShutdown
)(using Executor, ActorSystem, Scheduler, akka.stream.Materializer, lila.core.config.RateLimit):

  private val config = appConfig.get[FishnetConfig]("fishnet")(using AutoConfig.loader)

  private lazy val analysisColl = db(config.analysisColl)

  private lazy val redis = FishnetRedis(
    RedisClient.create(RedisURI.create(config.redisUri)),
    "fishnet-in",
    "fishnet-out",
    shutdown
  )

  private lazy val clientVersion = Client.ClientVersion(config.clientMinVersion)

  private lazy val repo = new FishnetRepo(
    analysisColl = analysisColl,
    clientColl = db(config.clientColl),
    cacheApi = cacheApi
  )

  private lazy val monitor: Monitor = wire[Monitor]

  private lazy val evalCache = wire[FishnetEvalCache]

  private lazy val analysisBuilder = wire[AnalysisBuilder]

  private lazy val apiConfig = FishnetApi.Config(offlineMode = config.offlineMode)

  private lazy val socketExists: GameId => Fu[Boolean] = id =>
    Bus.ask[Boolean, lila.core.round.SocketExists](lila.core.round.SocketExists(id, _))

  lazy val api: FishnetApi = wire[FishnetApi]

  lazy val openingBookDepth = settingStore[Int](
    "fishnetOpeningBookDepth",
    default = 0,
    text = "Fishnet: use opening explorer until ply".some
  ).taggedWith[FishnetOpeningBook.Depth]

  private lazy val openingBook: FishnetOpeningBook = wire[FishnetOpeningBook]

  lazy val player = wire[FishnetPlayer]

  private val limiter = wire[FishnetLimiter]

  lazy val analyser = wire[Analyser]

  lazy val awaiter = wire[FishnetAwaiter]

  wire[Cleaner]

  private def disable(keyOrUser: String) =
    repo.toKey(keyOrUser).flatMap { repo.enableClient(_, v = false) }

  def cli = new lila.common.Cli:
    def process =
      case "fishnet" :: "client" :: "create" :: name :: Nil =>
        userApi
          .enabledById(UserStr(name))
          .map(_.exists(_.marks.clean))
          .flatMap:
            if _ then
              api.createClient(UserStr(name).id).map { client =>
                Bus.pub(lila.core.fishnet.NewKey(client.userId, client.key.value))
                s"Created key: ${client.key.value} for: $name"
              }
            else fuccess("User missing, closed, or banned")
      case "fishnet" :: "client" :: "delete" :: key :: Nil =>
        repo.toKey(key).flatMap(repo.deleteClient).inject("done!")
      case "fishnet" :: "client" :: "enable" :: key :: Nil =>
        repo.toKey(key).flatMap { repo.enableClient(_, v = true) }.inject("done!")
      case "fishnet" :: "client" :: "disable" :: key :: Nil => disable(key).inject("done!")

  Bus.sub[lila.core.mod.MarkCheater]:
    case lila.core.mod.MarkCheater(userId, true) => disable(userId.value)
  Bus.sub[lila.core.mod.MarkBooster]:
    case lila.core.mod.MarkBooster(userId) => disable(userId.value)
  Bus.sub[lila.core.mod.Shadowban]:
    case lila.core.mod.Shadowban(userId, true) => disable(userId.value)

  Bus.sub[lila.core.fishnet.Bus]:
    case lila.core.fishnet.Bus.GameRequest(id) =>
      analyser(id, Work.Sender(userId = UserId.lichess, ip = none, mod = false, system = true))
    case req: lila.core.fishnet.Bus.StudyChapterRequest => analyser.study(req)

  Bus.sub[lila.core.fishnet.FishnetMoveRequest]: req =>
    player(req.game)
