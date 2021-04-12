package lila.fishnet

import akka.actor._
import com.softwaremill.macwire._
import io.lettuce.core._
import io.methvin.play.autoconfig._
import play.api.Configuration

import lila.common.Bus
import lila.common.config._
import lila.game.Game

@Module
private class FishnetConfig(
    @ConfigName("collection.analysis") val analysisColl: CollName,
    @ConfigName("collection.client") val clientColl: CollName,
    @ConfigName("actor.name") val actorName: String,
    @ConfigName("offline_mode") val offlineMode: Boolean,
    @ConfigName("analysis.nodes") val analysisNodes: Int,
    @ConfigName("move.plies") val movePlies: Int,
    @ConfigName("client_min_version") val clientMinVersion: String,
    @ConfigName("redis.uri") val redisUri: String
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
    sink: lila.analyse.Analyser,
    shutdown: akka.actor.CoordinatedShutdown
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  private val config = appConfig.get[FishnetConfig]("fishnet")(AutoConfig.loader)

  private lazy val analysisColl = db(config.analysisColl)

  private lazy val redis = new FishnetRedis(
    RedisClient create RedisURI.create(config.redisUri),
    "fishnet-in",
    "fishnet-out",
    shutdown
  )

  private lazy val clientVersion = new Client.ClientVersion(config.clientMinVersion)

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

  private lazy val socketExists: Game.ID => Fu[Boolean] = id =>
    Bus.ask[Boolean]("roundSocket")(lila.hub.actorApi.map.Exists(id, _))

  lazy val api: FishnetApi = wire[FishnetApi]

  lazy val player = {
    def mk = (plies: Int) => wire[Player]
    mk(config.movePlies)
  }

  private val limiter = wire[FishnetLimiter]

  lazy val analyser = wire[Analyser]

  lazy val awaiter = wire[FishnetAwaiter]

  lazy val aiPerfApi = wire[AiPerfApi]

  wire[Cleaner]

  // api actor
  system.actorOf(
    Props(new Actor {
      def receive = {
        case lila.hub.actorApi.fishnet.AutoAnalyse(gameId) =>
          analyser(
            gameId,
            Work.Sender(userId = lila.user.User.lichessId, ip = none, mod = false, system = true)
          ).unit
        case req: lila.hub.actorApi.fishnet.StudyChapterRequest => analyser.study(req).unit
      }
    }),
    name = config.actorName
  )

  private def disable(username: String) =
    repo toKey username flatMap { repo.enableClient(_, v = false) }

  def cli =
    new lila.common.Cli {
      def process = {
        case "fishnet" :: "client" :: "create" :: name :: Nil =>
          val userId = name.toLowerCase
          api.createClient(Client.UserId(userId)) map { client =>
            Bus.publish(lila.hub.actorApi.fishnet.NewKey(userId, client.key.value), "fishnet")
            s"Created key: ${client.key.value} for: $userId"
          }
        case "fishnet" :: "client" :: "delete" :: key :: Nil =>
          repo toKey key flatMap repo.deleteClient inject "done!"
        case "fishnet" :: "client" :: "enable" :: key :: Nil =>
          repo toKey key flatMap { repo.enableClient(_, v = true) } inject "done!"
        case "fishnet" :: "client" :: "disable" :: key :: Nil => disable(key) inject "done!"
      }
    }

  Bus.subscribeFun("adjustCheater", "adjustBooster", "shadowban") {
    case lila.hub.actorApi.mod.MarkCheater(userId, true) => disable(userId).unit
    case lila.hub.actorApi.mod.MarkBooster(userId)       => disable(userId).unit
    case lila.hub.actorApi.mod.Shadowban(userId, true)   => disable(userId).unit
  }
}
