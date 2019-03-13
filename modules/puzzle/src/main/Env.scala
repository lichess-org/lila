package lidraughts.puzzle

import akka.actor.{ ActorSelection, ActorSystem }
import com.typesafe.config.Config
import lidraughts.db.dsl.Coll
import draughts.variant.{ Variant, Standard, Frisian }

final class Env(
    config: Config,
    renderer: ActorSelection,
    lightUserApi: lidraughts.user.LightUserApi,
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    system: ActorSystem,
    lifecycle: play.api.inject.ApplicationLifecycle
) {

  private val settings = new {
    val CollectionPuzzle = config getString "collection.puzzle"
    val CollectionPuzzleFrisian = config getString "collection.puzzle_frisian"
    val CollectionRound = config getString "collection.round"
    val CollectionVote = config getString "collection.vote"
    val CollectionHead = config getString "collection.head"
    val ApiToken = config getString "api.token"
    val AnimationDuration = config duration "animation.duration"
    val PuzzleIdMin = config getInt "selector.puzzle_id_min"
  }
  import settings._

  private val db = new lidraughts.db.Env("puzzle", config getConfig "mongodb", lifecycle)

  private lazy val gameJson = new GameJson(asyncCache, lightUserApi)

  lazy val jsonView = new JsonView(
    gameJson,
    animationDuration = AnimationDuration
  )

  lazy val api = new PuzzleApi(
    puzzleColl = puzzleColl(Standard),
    roundColl = roundColl,
    voteColl = voteColl,
    headColl = headColl,
    puzzleIdMin = PuzzleIdMin,
    asyncCache = asyncCache,
    apiToken = ApiToken
  )

  lazy val finisher = new Finisher(
    api = api,
    puzzleColl = puzzleColl(Standard),
    bus = system.lidraughtsBus
  )

  lazy val selector = new Selector(
    puzzleColl = puzzleColl(Standard),
    api = api,
    puzzleIdMin = PuzzleIdMin
  )

  lazy val batch = new PuzzleBatch(
    puzzleColl = puzzleColl(Standard),
    api = api,
    finisher = finisher,
    puzzleIdMin = PuzzleIdMin
  )

  lazy val userInfos = UserInfos(roundColl = roundColl)

  lazy val forms = DataForm

  lazy val daily = new Daily(
    puzzleColl(Standard),
    renderer,
    asyncCache = asyncCache,
    system.scheduler
  )

  def cli = new lidraughts.common.Cli {
    def process = {
      case "puzzle" :: "disable" :: id :: Nil => parseIntOption(id) ?? { id =>
        api.puzzle disable id inject "Done"
      }
    }
  }

  private[puzzle] lazy val puzzleColl: Map[Variant, Coll] = Map(Standard -> db(CollectionPuzzle), Frisian -> db(CollectionPuzzleFrisian))
  private[puzzle] lazy val roundColl = db(CollectionRound)
  private[puzzle] lazy val voteColl = db(CollectionVote)
  private[puzzle] lazy val headColl = db(CollectionHead)
}

object Env {

  lazy val current: Env = "puzzle" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "puzzle",
    renderer = lidraughts.hub.Env.current.actor.renderer,
    lightUserApi = lidraughts.user.Env.current.lightUserApi,
    asyncCache = lidraughts.memo.Env.current.asyncCache,
    system = lidraughts.common.PlayApp.system,
    lifecycle = lidraughts.common.PlayApp.lifecycle
  )
}
