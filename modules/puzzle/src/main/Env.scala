package lila.puzzle

import akka.actor.ActorSystem
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import scala.concurrent.duration.FiniteDuration

import lila.common.config._

@Module
private class PuzzleConfig(
    @ConfigName("mongodb.uri") val mongoUri: String,
    @ConfigName("collection.puzzle") val puzzleColl: CollName,
    @ConfigName("collection.round") val roundColl: CollName,
    @ConfigName("collection.vote") val voteColl: CollName,
    @ConfigName("collection.head") val headColl: CollName,
    @ConfigName("api.token") val apiToken: Secret,
    @ConfigName("animation.duration") val animationDuration: FiniteDuration
)

case class RoundRepo(coll: lila.db.AsyncColl)

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
    system: ActorSystem
) {

  private val config = appConfig.get[PuzzleConfig]("puzzle")(AutoConfig.loader)

  private lazy val db    = mongo.asyncDb("puzzle", config.mongoUri)
  private def puzzleColl = db(config.puzzleColl)
  private def roundColl  = db(config.roundColl)
  private def voteColl   = db(config.voteColl)
  private def headColl   = db(config.headColl)

  private lazy val gameJson: GameJson = wire[GameJson]

  lazy val jsonView = wire[JsonView]

  lazy val api = new PuzzleApi(
    puzzleColl = puzzleColl,
    roundColl = roundColl,
    voteColl = voteColl,
    headColl = headColl,
    cacheApi = cacheApi
  )

  lazy val roundRepo = RoundRepo(roundColl)

  lazy val finisher = new Finisher(
    historyApi = historyApi,
    userRepo = userRepo,
    api = api,
    puzzleColl = puzzleColl
  )

  lazy val forms = PuzzleForm

  lazy val daily = new Daily(
    puzzleColl,
    renderer,
    cacheApi = cacheApi
  )

  lazy val activity = new PuzzleActivity(
    puzzleColl = puzzleColl,
    roundColl = roundColl
  )

  def cli =
    new lila.common.Cli {
      def process = { case "puzzle" :: "delete" :: id :: Nil =>
        api.puzzle delete Puzzle.Id(id) inject "Done"
      }
    }
}
