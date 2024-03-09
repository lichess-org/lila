package lila.fishnet

import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import lila.db.dsl._

import lila.common.Bus
import lila.common.config._
import lila.game.Game

@Module
private class FishnetConfig(
    @ConfigName("collection.analysis") val analysisColl: CollName,
    @ConfigName("collection.puzzle") val puzzleColl: CollName,
    @ConfigName("collection.client") val clientColl: CollName,
    @ConfigName("actor.name") val actorName: String,
    @ConfigName("offline_mode") val offlineMode: Boolean,
    @ConfigName("analysis.nodes") val analysisNodes: Int,
    @ConfigName("move.plies") val movePlies: Int,
    @ConfigName("client_min_version") val clientMinVersion: String
)

case class FishnetColls(
    analysis: Coll,
    puzzle: Coll,
    client: Coll
)

@Module
final class Env(
    appConfig: Configuration,
    requesterApi: lila.analyse.RequesterApi,
    evalCacheApi: lila.evalCache.EvalCacheApi,
    gameRepo: lila.game.GameRepo,
    analysisRepo: lila.analyse.AnalysisRepo,
    db: lila.db.Db,
    cacheApi: lila.memo.CacheApi,
    sink: lila.analyse.Analyser,
    puzzle: lila.puzzle.PuzzleApi
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  private val config = appConfig.get[FishnetConfig]("fishnet")(AutoConfig.loader)

  private val analysisColl = db(config.analysisColl)

  private lazy val colls = FishnetColls(
    analysis = analysisColl,
    puzzle = db(config.puzzleColl),
    client = db(config.clientColl)
  )

  private lazy val repo = new FishnetRepo(
    colls = colls,
    cacheApi = cacheApi
  )

  private lazy val moveDb: MoveDB = wire[MoveDB]

  private lazy val monitor: Monitor = wire[Monitor]

  private lazy val evalCache = wire[FishnetEvalCache]

  private lazy val analysisBuilder = wire[AnalysisBuilder]

  private lazy val apiConfig = FishnetApi.Config(
    offlineMode = config.offlineMode,
    analysisNodes = config.analysisNodes,
    clientVersion = new Client.ClientVersion(config.clientMinVersion)
  )

  private lazy val socketExists: Game.ID => Fu[Boolean] = id =>
    Bus.ask[Boolean]("roundSocket")(lila.hub.actorApi.map.Exists(id, _))

  lazy val api: FishnetApi = wire[FishnetApi]

  lazy val player = {
    def mk = (plies: Int) => wire[Player]
    mk(config.movePlies)
  }

  private val limiter = wire[Limiter]

  lazy val analyser = wire[Analyser]

  wire[Cleaner]

  wire[MainWatcher]

  // api actor
  system.actorOf(
    Props(new Actor {
      def receive = {
        case lila.hub.actorApi.fishnet.AutoAnalyse(gameId) =>
          analyser(
            gameId,
            Work.Sender(userId = none, postGameStudy = none, ip = none, mod = false, system = true)
          ).unit
        case lila.hub.actorApi.fishnet.PostGameStudyRequest(userId, gameId, studyId, chapterId) =>
          analyser
            .postGameStudy(
              gameId,
              Work.Sender(
                userId = userId.some,
                postGameStudy = lila.analyse.Analysis.PostGameStudy(studyId, chapterId).some,
                ip = none,
                mod = false,
                system = false
              )
            )
            .unit
        case req: lila.hub.actorApi.fishnet.StudyChapterRequest => analyser.study(req).unit
      }
    }),
    name = config.actorName
  )

  private def disable(username: String) =
    repo toKey username flatMap { repo.enableClient(_, false) }

  def cli =
    new lila.common.Cli {
      def process = {
        case "fishnet" :: "client" :: "create" :: userId :: Nil =>
          api.createClient(Client.UserId(userId.toLowerCase)) map { client =>
            Bus.publish(lila.hub.actorApi.fishnet.NewKey(userId, client.key.value), "fishnet")
            s"Created key: ${(client.key.value)} for: $userId"
          }
        case "fishnet" :: "client" :: "delete" :: key :: Nil =>
          repo toKey key flatMap repo.deleteClient inject "done!"
        case "fishnet" :: "client" :: "enable" :: key :: Nil =>
          repo toKey key flatMap { repo.enableClient(_, true) } inject "done!"
        case "fishnet" :: "client" :: "disable" :: key :: Nil => disable(key) inject "done!"
      }
    }

  Bus.subscribeFun("adjustCheater", "adjustBooster", "shadowban") {
    case lila.hub.actorApi.mod.MarkCheater(userId, true) => disable(userId).unit
    case lila.hub.actorApi.mod.MarkBooster(userId)       => disable(userId).unit
    case lila.hub.actorApi.mod.Shadowban(userId, true)   => disable(userId).unit
  }
}
