package lila.puzzle

import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.autoconfig.{ *, given }
import lila.core.config.*
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
    historyApi: lila.core.history.HistoryApi,
    lightUserApi: lila.core.user.LightUserApi,
    userApi: lila.core.user.UserApi,
    cacheApi: lila.memo.CacheApi,
    mongoCacheApi: lila.memo.MongoCache.Api,
    gameRepo: lila.core.game.GameRepo,
    myEngines: lila.core.misc.analysis.MyEnginesAsJson,
    mongo: lila.db.Env
)(using Executor, akka.actor.ActorSystem, akka.stream.Materializer, lila.core.i18n.Translator)(using
    scheduler: Scheduler,
    mode: play.api.Mode
):

  private val config = appConfig.get[PuzzleConfig]("puzzle")(using AutoConfig.loader)

  private val db = mongo.asyncDb("puzzle", config.mongoUri)

  val colls = PuzzleColls(
    puzzle = db(config.puzzleColl),
    round = db(config.roundColl),
    path = db(config.pathColl)
  )

  private val gameJson: GameJson = wire[GameJson]

  val jsonView = wire[JsonView]

  private val pathApi = wire[PuzzlePathApi]

  private val trustApi = wire[PuzzleTrustApi]

  val opening = wire[PuzzleOpeningApi]

  private val countApi = wire[PuzzleCountApi]

  val api: PuzzleApi = wire[PuzzleApi]

  val session: PuzzleSessionApi = wire[PuzzleSessionApi]

  val anon: PuzzleAnon = wire[PuzzleAnon]

  val selector: PuzzleSelector = wire[PuzzleSelector]

  val batch: PuzzleBatch = wire[PuzzleBatch]

  val finisher = wire[PuzzleFinisher]

  val forms = PuzzleForm

  val daily = wire[DailyPuzzle]

  val activity = wire[PuzzleActivity]

  val dashboard = wire[PuzzleDashboardApi]

  val replay = wire[PuzzleReplayApi]

  val history = wire[PuzzleHistoryApi]

  val streak = wire[PuzzleStreakApi]

  val complete = wire[PuzzleComplete]

  private val tagger = wire[PuzzleTagger]

  val tryDailyPuzzle: lila.puzzle.DailyPuzzle.Try = () =>
    Future {
      daily.get
    }.flatMap(identity)
      .withTimeoutDefault(50.millis, none)
      .recover { case e: Exception =>
        logger.warn("daily", e)
        none
      }

  lila.common.Cli.handle:
    case "puzzle" :: "opening" :: "recompute" :: "all" :: Nil =>
      opening.recomputeAll
      fuccess("started in background")
    case "puzzle" :: "issue" :: id :: issue :: Nil =>
      api.puzzle.setIssue(PuzzleId(id), issue).map(if _ then "done" else "not found")

  scheduler.scheduleAtFixedRate(10.minutes, 1.day): () =>
    tagger.addAllMissing

  if mode.isProd then
    scheduler.scheduleAtFixedRate(10.minutes, 10.minutes): () =>
      pathApi.isStale.foreach: stale =>
        if stale then logger.error("Puzzle paths appear to be stale! check that the regen cron is up")

final class PuzzleColls(
    val puzzle: AsyncColl,
    val round: AsyncColl,
    val path: AsyncColl
)
