package lila.puzzle

import akka.actor.{ ActorSelection, ActorSystem }
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    renderer: ActorSelection,
    system: ActorSystem,
    lifecycle: play.api.inject.ApplicationLifecycle) {

  private val settings = new {
    val CollectionPuzzle = config getString "collection.puzzle"
    val CollectionRound = config getString "collection.round"
    val CollectionLearning = config getString "collection.learning"
    val CollectionVote = config getString "collection.vote"
    val CollectionHead = config getString "collection.head"
    val ApiToken = config getString "api.token"
    val PngExecPath = config getString "png.exec_path"
  }
  import settings._

  val AnimationDuration = config duration "animation.duration"

  private val db = new lila.db.Env("puzzle", config getConfig "mongodb", lifecycle)

  lazy val api = new PuzzleApi(
    puzzleColl = puzzleColl,
    roundColl = roundColl,
    learningColl = learningColl,
    voteColl = voteColl,
    headColl = headColl,
    apiToken = ApiToken)

  lazy val finisher = new Finisher(
    api = api,
    puzzleColl = puzzleColl)

  lazy val selector = new Selector(
    puzzleColl = puzzleColl,
    api = api,
    anonMinRating = config getInt "selector.anon_min_rating",
    puzzleIdMin = config getInt "selector.puzzle_id_min")

  lazy val userInfos = UserInfos(roundColl = roundColl)

  lazy val forms = DataForm

  lazy val daily = new Daily(
    puzzleColl,
    renderer,
    system.scheduler
  ).apply _

  lazy val pngExport = PngExport(PngExecPath) _

  def cli = new lila.common.Cli {
    def process = {
      case "puzzle" :: "export" :: nbStr :: Nil => parseIntOption(nbStr) ?? { nb =>
        Export(api, nb)
      }
      case "puzzle" :: "disable" :: id :: Nil => parseIntOption(id) ?? { id =>
        api.puzzle disable id inject "Done"
      }
    }
  }

  private[puzzle] lazy val puzzleColl = db(CollectionPuzzle)
  private[puzzle] lazy val roundColl = db(CollectionRound)
  private[puzzle] lazy val learningColl = db(CollectionLearning)
  private[puzzle] lazy val voteColl = db(CollectionVote)
  private[puzzle] lazy val headColl = db(CollectionHead)
}

object Env {

  lazy val current: Env = "puzzle" boot new Env(
    config = lila.common.PlayApp loadConfig "puzzle",
    renderer = lila.hub.Env.current.actor.renderer,
    system = lila.common.PlayApp.system,
    lifecycle = lila.common.PlayApp.lifecycle)
}
