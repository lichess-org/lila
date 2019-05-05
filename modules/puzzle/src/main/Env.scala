package lila.puzzle

import akka.actor.{ ActorSelection, ActorSystem }
import com.typesafe.config.Config

final class Env(
    config: Config,
    renderer: ActorSelection,
    historyApi: lila.history.HistoryApi,
    lightUserApi: lila.user.LightUserApi,
    asyncCache: lila.memo.AsyncCache.Builder,
    system: ActorSystem,
    lifecycle: play.api.inject.ApplicationLifecycle
) {

  private val settings = new {
    val CollectionPuzzle = config getString "collection.puzzle"
    val CollectionRound = config getString "collection.round"
    val CollectionVote = config getString "collection.vote"
    val CollectionHead = config getString "collection.head"
    val ApiToken = config getString "api.token"
    val AnimationDuration = config duration "animation.duration"
    val PuzzleIdMin = config getInt "selector.puzzle_id_min"
  }
  import settings._

  private val db = new lila.db.Env("puzzle", config getConfig "mongodb", lifecycle)

  private lazy val gameJson = new GameJson(asyncCache, lightUserApi)

  lazy val jsonView = new JsonView(
    gameJson,
    animationDuration = AnimationDuration
  )

  lazy val api = new PuzzleApi(
    puzzleColl = puzzleColl,
    roundColl = roundColl,
    voteColl = voteColl,
    headColl = headColl,
    puzzleIdMin = PuzzleIdMin,
    asyncCache = asyncCache,
    apiToken = ApiToken
  )

  lazy val finisher = new Finisher(
    historyApi = historyApi,
    api = api,
    puzzleColl = puzzleColl,
    bus = system.lilaBus
  )

  lazy val selector = new Selector(
    puzzleColl = puzzleColl,
    api = api,
    puzzleIdMin = PuzzleIdMin
  )

  lazy val batch = new PuzzleBatch(
    puzzleColl = puzzleColl,
    api = api,
    finisher = finisher,
    puzzleIdMin = PuzzleIdMin
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
      case "puzzle" :: "disable" :: id :: Nil => parseIntOption(id) ?? { id =>
        api.puzzle disable id inject "Done"
      }
    }
  }

  private[puzzle] lazy val puzzleColl = db(CollectionPuzzle)
  private[puzzle] lazy val roundColl = db(CollectionRound)
  private[puzzle] lazy val voteColl = db(CollectionVote)
  private[puzzle] lazy val headColl = db(CollectionHead)
}

object Env {

  lazy val current: Env = "puzzle" boot new Env(
    config = lila.common.PlayApp loadConfig "puzzle",
    renderer = lila.hub.Env.current.renderer,
    historyApi = lila.history.Env.current.api,
    lightUserApi = lila.user.Env.current.lightUserApi,
    asyncCache = lila.memo.Env.current.asyncCache,
    system = lila.common.PlayApp.system,
    lifecycle = lila.common.PlayApp.lifecycle
  )
}
