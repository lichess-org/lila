package lila.puzzle

import akka.actor.ActorSystem
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import scala.concurrent.duration.FiniteDuration
import play.api.Configuration

import lila.common.config._
import lila.db.Env.configLoader

@Module
private class CoachConfig(
    val mongodb: lila.db.DbConfig,
    @ConfigName("collection.puzzle") val puzzleColl: CollName,
    @ConfigName("collection.round") val roundColl: CollName,
    @ConfigName("collection.vote") val voteColl: CollName,
    @ConfigName("collection.head") val headColl: CollName,
    @ConfigName("api.token") val apiToken: Secret,
    @ConfigName("animation.duration") val animationDuration: FiniteDuration,
    @ConfigName("selector.puzzle_id_min") val puzzleIdMin: Int
)

final class Env(
    appConfig: Configuration,
    renderer: lila.hub.actors.Renderer,
    historyApi: lila.history.HistoryApi,
    lightUserApi: lila.user.LightUserApi,
    asyncCache: lila.memo.AsyncCache.Builder,
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo
)(implicit system: ActorSystem) {

  private val config = appConfig.get[CoachConfig]("coach")(AutoConfig.loader)

  private lazy val db = new lila.db.Env("puzzle", config.mongodb)

  private lazy val gameJson = wire[GameJson]

  lazy val jsonView = wire[JsonView]

  lazy val api = new PuzzleApi(
    puzzleColl = puzzleColl,
    roundColl = roundColl,
    voteColl = voteColl,
    headColl = headColl,
    puzzleIdMin = config.puzzleIdMin,
    asyncCache = asyncCache,
    apiToken = config.apiToken
  )

  lazy val finisher = new Finisher(
    historyApi = historyApi,
    userRepo = userRepo,
    api = api,
    puzzleColl = puzzleColl
  )

  lazy val selector = new Selector(
    puzzleColl = puzzleColl,
    api = api,
    puzzleIdMin = config.puzzleIdMin
  )

  lazy val batch = new PuzzleBatch(
    puzzleColl = puzzleColl,
    api = api,
    finisher = finisher,
    puzzleIdMin = config.puzzleIdMin
  )

  lazy val userInfos = new UserInfosApi(
    roundColl = roundColl,
    currentPuzzleId = api.head.currentPuzzleId
  )

  lazy val forms = DataForm

  lazy val daily = new Daily(
    puzzleColl,
    renderer,
    asyncCache = asyncCache,
    system.scheduler
  )

  lazy val activity = new PuzzleActivity(
    puzzleColl = puzzleColl,
    roundColl = roundColl
  )(system)

  def cli = new lila.common.Cli {
    def process = {
      case "puzzle" :: "disable" :: id :: Nil => id.toIntOption ?? { id =>
        api.puzzle disable id inject "Done"
      }
    }
  }

  private lazy val puzzleColl = db(config.puzzleColl)
  private lazy val roundColl = db(config.roundColl)
  private lazy val voteColl = db(config.voteColl)
  private lazy val headColl = db(config.headColl)
}
