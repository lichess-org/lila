package lila.puzzle

import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import scala.concurrent.duration.FiniteDuration

import lila.common.config._
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
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    mongo: lila.db.Env
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem,
    mode: play.api.Mode
) {

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

  def cli =
    new lila.common.Cli {
      def process = { case "puzzle" :: "delete" :: id :: Nil =>
        api.puzzle delete Puzzle.Id(id) inject "Done"
      }
    }
}

final class PuzzleColls(
    val puzzle: AsyncColl,
    val round: AsyncColl,
    val path: AsyncColl
)
