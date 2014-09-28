package lila.puzzle

import akka.actor.{ ActorSelection, ActorSystem, Props }
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
    anonMinRating = config getInt "selector.anon_min_rating",
    toleranceStep = config getInt "selector.tolerance.step",
    toleranceMax = config getInt "selector.tolerance.max",
    modulo = config getInt "selector.modulo")

  lazy val userInfos = UserInfos(attemptColl = attemptColl)

  lazy val forms = DataForm

  lazy val daily = new Daily(
    puzzleColl,
    renderer,
    system.scheduler
  ).apply _

  private[puzzle] lazy val puzzleColl = db(CollectionPuzzle)
  private[puzzle] lazy val attemptColl = db(CollectionAttempt)
}

object Env {

  lazy val current: Env = "[boot] puzzle" describes new Env(
    config = lila.common.PlayApp loadConfig "puzzle",
    db = lila.db.Env.current,
    renderer = lila.hub.Env.current.actor.renderer,
    system = lila.common.PlayApp.system)
}
