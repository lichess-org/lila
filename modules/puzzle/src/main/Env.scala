package lila.puzzle

import com.softwaremill.macwire.*
import lila.common.autoconfig.{ *, given }
import play.api.Configuration

import lila.common.config.*
import lila.db.AsyncColl

@Module
private class PuzzleConfig(
    @ConfigName("mongodb.uri") val mongoUri: String,
    @ConfigName("collection.puzzle") val puzzleColl: CollName,
    @ConfigName("collection.round") val roundColl: CollName,
    @ConfigName("collection.path") val pathColl: CollName
)

@Module
final class Env(
    appConfig: Configuration,
    renderer: lila.hub.actors.Renderer,
    historyApi: lila.history.HistoryApi,
    lightUserApi: lila.user.LightUserApi,
    cacheApi: lila.memo.CacheApi,
    mongoCacheApi: lila.memo.MongoCache.Api,
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    perfsRepo: lila.user.UserPerfsRepo,
    mongo: lila.db.Env
)(using
    ec: Executor,
    system: akka.actor.ActorSystem,
    scheduler: Scheduler,
    materializer: akka.stream.Materializer,
    mode: play.api.Mode
):

  private val config = appConfig.get[PuzzleConfig]("puzzle")(AutoConfig.loader)

  private lazy val db = mongo.asyncDb("puzzle", config.mongoUri)

  lazy val colls = new PuzzleColls(
    puzzle = db(config.puzzleColl),
    round = db(config.roundColl),
    path = db(config.pathColl)
  )

  private lazy val gameJson: GameJson = wire[GameJson]

  lazy val jsonView = wire[JsonView]

  private lazy val pathApi = wire[PuzzlePathApi]

  private lazy val trustApi = wire[PuzzleTrustApi]

  private lazy val countApi = wire[PuzzleCountApi]

  lazy val api: PuzzleApi = wire[PuzzleApi]

  lazy val session: PuzzleSessionApi = wire[PuzzleSessionApi]

  lazy val selector: PuzzleSelector = wire[PuzzleSelector]

  lazy val anon: PuzzleAnon = wire[PuzzleAnon]

  lazy val batch: PuzzleBatch = wire[PuzzleBatch]

  lazy val finisher = wire[PuzzleFinisher]

  lazy val forms = PuzzleForm

  lazy val daily = wire[DailyPuzzle]

  lazy val activity = wire[PuzzleActivity]

  lazy val dashboard = wire[PuzzleDashboardApi]

  lazy val replay = wire[PuzzleReplayApi]

  lazy val history = wire[PuzzleHistoryApi]

  lazy val streak = wire[PuzzleStreakApi]

  lazy val opening = wire[PuzzleOpeningApi]

  private lazy val tagger = wire[PuzzleTagger]

  scheduler.scheduleAtFixedRate(10 minutes, 1 day): () =>
    tagger.addAllMissing

  if mode == play.api.Mode.Prod then
    scheduler.scheduleAtFixedRate(1 hour, 1 hour): () =>
      pathApi.isStale.foreach: stale =>
        if stale then logger.error("Puzzle paths appear to be stale! check that the regen cron is up")

final class PuzzleColls(
    val puzzle: AsyncColl,
    val round: AsyncColl,
    val path: AsyncColl
)
