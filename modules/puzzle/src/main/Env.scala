package lidraughts.puzzle

import akka.actor.{ ActorSelection, ActorSystem }
import com.typesafe.config.Config
import lidraughts.db.dsl.Coll
import draughts.variant.{ Variant, Standard, Frisian, Russian }

final class Env(
    config: Config,
    renderer: ActorSelection,
    historyApi: lidraughts.history.HistoryApi,
    lightUserApi: lidraughts.user.LightUserApi,
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    system: ActorSystem,
    lifecycle: play.api.inject.ApplicationLifecycle
) {

  private val settings = new {
    val CollectionPuzzle = config getString "collection.puzzle"
    val CollectionPuzzleFrisian = config getString "collection.puzzle_frisian"
    val CollectionPuzzleRussian = config getString "collection.puzzle_russian"
    val CollectionRound = config getString "collection.round"
    val CollectionRoundFrisian = config getString "collection.round_frisian"
    val CollectionRoundRussian = config getString "collection.round_russian"
    val CollectionVote = config getString "collection.vote"
    val CollectionVoteFrisian = config getString "collection.vote_frisian"
    val CollectionVoteRussian = config getString "collection.vote_russian"
    val CollectionHead = config getString "collection.head"
    val CollectionHeadFrisian = config getString "collection.head_frisian"
    val CollectionHeadRussian = config getString "collection.head_russian"
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
    bus = system.lidraughtsBus
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

  lazy val userInfos = UserInfos(roundColl = roundColl)

  lazy val forms = DataForm

  lazy val daily = new Daily(
    puzzleColl(Standard),
    renderer,
    asyncCache = asyncCache,
    system.scheduler
  )

  lazy val activity = new PuzzleActivity(
    puzzleColl = puzzleColl(Standard),
    roundColl = roundColl(Standard)
  )(system)

  def cli = new lidraughts.common.Cli {
    def process = {
      case "puzzle" :: "disable" :: key :: id :: Nil if key == "standard" || key == "frisian" =>
        Variant.byKey.get(key) ?? { v =>
          parseIntOption(id) ?? { id =>
            api.puzzle.disable(v, id) inject "Done"
          }
        }
    }
  }

  private[puzzle] lazy val puzzleColl: Map[Variant, Coll] = Map(Standard -> db(CollectionPuzzle), Frisian -> db(CollectionPuzzleFrisian), Russian -> db(CollectionPuzzleRussian))
  private[puzzle] lazy val roundColl: Map[Variant, Coll] = Map(Standard -> db(CollectionRound), Frisian -> db(CollectionRoundFrisian), Russian -> db(CollectionRoundRussian))
  private[puzzle] lazy val voteColl: Map[Variant, Coll] = Map(Standard -> db(CollectionVote), Frisian -> db(CollectionVoteFrisian), Russian -> db(CollectionVoteRussian))
  private[puzzle] lazy val headColl: Map[Variant, Coll] = Map(Standard -> db(CollectionHead), Frisian -> db(CollectionHeadFrisian), Russian -> db(CollectionHeadRussian))
}

object Env {

  lazy val current: Env = "puzzle" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "puzzle",
    renderer = lidraughts.hub.Env.current.renderer,
    historyApi = lidraughts.history.Env.current.api,
    lightUserApi = lidraughts.user.Env.current.lightUserApi,
    asyncCache = lidraughts.memo.Env.current.asyncCache,
    system = lidraughts.common.PlayApp.system,
    lifecycle = lidraughts.common.PlayApp.lifecycle
  )
}
