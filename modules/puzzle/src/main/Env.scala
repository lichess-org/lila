package lila.puzzle

import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.autoconfig.{ *, given }
import lila.common.config.*
import lila.db.AsyncColl
import lila.core.config.*

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
    historyApi: lila.core.history.HistoryApi,
    lightUserApi: lila.core.user.LightUserApi,
    userApi: lila.core.user.UserApi,
    cacheApi: lila.memo.CacheApi,
    mongoCacheApi: lila.memo.MongoCache.Api,
    gameRepo: lila.game.GameRepo,
    mongo: lila.db.Env
)(using Executor, akka.actor.ActorSystem, akka.stream.Materializer, lila.core.i18n.Translator)(using
    scheduler: Scheduler,
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

  def cli = new lila.common.Cli:
    def process =
      case "puzzle" :: "opening" :: "recompute" :: "all" :: Nil =>
        opening.recomputeAll
        fuccess("started in background")
      case "puzzle" :: "issue" :: id :: issue :: Nil =>
        api.puzzle
          .setIssue(PuzzleId(id), issue)
          .map: res =>
            if res then "done" else "not found"

  scheduler.scheduleAtFixedRate(10 minutes, 1 day): () =>
    tagger.addAllMissing

  if mode.isProd then
    scheduler.scheduleAtFixedRate(1 hour, 1 hour): () =>
      pathApi.isStale.foreach: stale =>
        if stale then logger.error("Puzzle paths appear to be stale! check that the regen cron is up")

final class PuzzleColls(
    val puzzle: AsyncColl,
    val round: AsyncColl,
    val path: AsyncColl
)
