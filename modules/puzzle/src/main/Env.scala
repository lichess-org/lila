package lila.puzzle

import akka.actor.{ ActorSelection, ActorSystem }
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    renderer: ActorSelection,
    system: ActorSystem) {

  private val settings = new {
    val CollectionPuzzle = config getString "collection.puzzle"
    val CollectionAttempt = config getString "collection.attempt"
    val ApiToken = config getString "api.token"
    val PngExecPath = config getString "png.exec_path"
  }
  import settings._

  val AnimationDuration = config duration "animation.duration"

  lazy val api = new PuzzleApi(
    puzzleColl = puzzleColl,
    attemptColl = attemptColl,
    apiToken = ApiToken)

  lazy val finisher = new Finisher(
    api = api,
    puzzleColl = puzzleColl)

  lazy val selector = new Selector(
    puzzleColl = puzzleColl,
    api = api,
    scheduler = system.scheduler,
    anonMinRating = config getInt "selector.anon_min_rating",
    maxAttempts = config getInt "selector.max_attempts")

  lazy val userInfos = UserInfos(attemptColl = attemptColl)

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
    }
  }

  private[puzzle] lazy val puzzleColl = db(CollectionPuzzle)
  private[puzzle] lazy val attemptColl = db(CollectionAttempt)
}

object Env {

  lazy val current: Env = "puzzle" boot new Env(
    config = lila.common.PlayApp loadConfig "puzzle",
    db = lila.db.Env.current,
    renderer = lila.hub.Env.current.actor.renderer,
    system = lila.common.PlayApp.system)
}
