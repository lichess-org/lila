package lila.fishnet

import akka.actor.*
import com.softwaremill.macwire.*
import com.softwaremill.tagging.*
import io.lettuce.core.*
import lila.common.autoconfig.{ *, given }
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient

import lila.common.Bus
import lila.common.config.*

@Module
private class FishnetConfig(
    @ConfigName("collection.analysis") val analysisColl: CollName,
    @ConfigName("collection.client") val clientColl: CollName,
    @ConfigName("actor.name") val actorName: String,
    @ConfigName("offline_mode") val offlineMode: Boolean,
    @ConfigName("analysis.nodes") val analysisNodes: Int,
    @ConfigName("move.plies") val movePlies: Int,
    @ConfigName("client_min_version") val clientMinVersion: String,
    @ConfigName("redis.uri") val redisUri: String,
    val explorerEndpoint: String
)

@Module
final class Env(
    appConfig: Configuration,
    uciMemo: lila.game.UciMemo,
    requesterApi: lila.analyse.RequesterApi,
    evalCacheApi: lila.evalCache.EvalCacheApi,
    gameRepo: lila.game.GameRepo,
    analysisRepo: lila.analyse.AnalysisRepo,
    db: lila.db.Db,
    cacheApi: lila.memo.CacheApi,
    settingStore: lila.memo.SettingStore.Builder,
    ws: StandaloneWSClient,
    sink: lila.analyse.Analyser,
    userRepo: lila.user.UserRepo,
    shutdown: akka.actor.CoordinatedShutdown
)(using
    ec: Executor,
    system: ActorSystem,
    scheduler: Scheduler,
    materializer: akka.stream.Materializer
):

  private val config = appConfig.get[FishnetConfig]("fishnet")(AutoConfig.loader)

  private lazy val analysisColl = db(config.analysisColl)

  private lazy val redis = FishnetRedis(
    RedisClient create RedisURI.create(config.redisUri),
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

  private lazy val apiConfig = FishnetApi.Config(
    offlineMode = config.offlineMode,
    analysisNodes = config.analysisNodes
  )

  private lazy val socketExists: GameId => Fu[Boolean] = id =>
    Bus.ask[Boolean]("roundSocket")(lila.hub.actorApi.map.Exists(id.value, _))

  lazy val api: FishnetApi = wire[FishnetApi]

  lazy val openingBookDepth = settingStore[Int](
    "fishnetOpeningBookDepth",
    default = 0,
    text = "Fishnet: use opening explorer until ply".some
  ).taggedWith[FishnetOpeningBook.Depth]

  private lazy val openingBook: FishnetOpeningBook = wire[FishnetOpeningBook]

  lazy val player =
    def mk = (plies: Int) => wire[FishnetPlayer]
    mk(config.movePlies)

  private val limiter = wire[FishnetLimiter]

  lazy val analyser = wire[Analyser]

  lazy val awaiter = wire[FishnetAwaiter]

  wire[Cleaner]

  // api actor
  system.actorOf(
    Props(
      new Actor:
        def receive =
          case lila.hub.actorApi.fishnet.AutoAnalyse(gameId) =>
            val sender = Work.Sender(userId = lila.user.User.lichessId, ip = none, mod = false, system = true)
            analyser(gameId, sender)
          case req: lila.hub.actorApi.fishnet.StudyChapterRequest => analyser.study(req)
    ),
    name = config.actorName
  )

  private def disable(keyOrUser: String) =
    repo toKey keyOrUser flatMap { repo.enableClient(_, v = false) }

  def cli = new lila.common.Cli:
    def process =
      case "fishnet" :: "client" :: "create" :: name :: Nil =>
        userRepo.enabledById(UserStr(name)).map(_.exists(_.marks.clean)) flatMap {
          if _ then
            api.createClient(UserStr(name).id) map { client =>
              Bus.publish(lila.hub.actorApi.fishnet.NewKey(client.userId, client.key.value), "fishnet")
              s"Created key: ${client.key.value} for: $name"
            }
          else fuccess("User missing, closed, or banned")
        }
      case "fishnet" :: "client" :: "delete" :: key :: Nil =>
        repo toKey key flatMap repo.deleteClient inject "done!"
      case "fishnet" :: "client" :: "enable" :: key :: Nil =>
        repo toKey key flatMap { repo.enableClient(_, v = true) } inject "done!"
      case "fishnet" :: "client" :: "disable" :: key :: Nil => disable(key) inject "done!"

  Bus.subscribeFun("adjustCheater", "adjustBooster", "shadowban"):
    case lila.hub.actorApi.mod.MarkCheater(userId, true) => disable(userId.value)
    case lila.hub.actorApi.mod.MarkBooster(userId)       => disable(userId.value)
    case lila.hub.actorApi.mod.Shadowban(userId, true)   => disable(userId.value)
