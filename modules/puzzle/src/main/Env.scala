package lila.puzzle

import akka.actor.{ ActorSystem, Props }
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    system: ActorSystem) {

  private val settings = new {
    val CollectionPuzzle = config getString "collection.puzzle"
    val CollectionAttempt = config getString "collection.attempt"
    val ApiToken = config getString "api.token"
  }
  import settings._

  lazy val api = new PuzzleApi(
    puzzleColl = puzzleColl,
    attemptColl = attemptColl,
    apiToken = ApiToken)

  lazy val forms = DataForm

  def cli = new lila.common.Cli {
    def process = {
      case "puzzle" :: "fix" :: "fen" :: Nil ⇒ api.fixAll inject "fixed!"
    }
  }

  private[puzzle] lazy val puzzleColl = db(CollectionPuzzle)
  private[puzzle] lazy val attemptColl = db(CollectionAttempt)
}

object Env {

  lazy val current: Env = "[boot] puzzle" describes new Env(
    config = lila.common.PlayApp loadConfig "puzzle",
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system)
}
