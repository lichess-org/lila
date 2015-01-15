package lila.opening

import akka.actor.{ ActorSelection, ActorSystem }
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env) {

  private val settings = new {
    val CollectionOpening = config getString "collection.opening"
    val CollectionAttempt = config getString "collection.attempt"
    val CollectionName = config getString "collection.name"
    val ApiToken = config getString "api.token"
  }
  import settings._

  val AnimationDuration = config duration "animation.duration"

  lazy val api = new OpeningApi(
    openingColl = openingColl,
    attemptColl = attemptColl,
    nameColl = nameColl,
    apiToken = ApiToken)

  lazy val selector = new Selector(
    openingColl = openingColl,
    api = api,
    toleranceStep = config getInt "selector.tolerance.step",
    toleranceMax = config getInt "selector.tolerance.max",
    modulo = config getInt "selector.modulo")

  lazy val finisher = new Finisher(
    api = api,
    openingColl = openingColl)

  lazy val userInfos = UserInfos(attemptColl = attemptColl)

  private[opening] lazy val openingColl = db(CollectionOpening)
  private[opening] lazy val attemptColl = db(CollectionAttempt)
  private[opening] lazy val nameColl = db(CollectionName)
}

object Env {

  lazy val current: Env = "[boot] opening" describes new Env(
    config = lila.common.PlayApp loadConfig "opening",
    db = lila.db.Env.current)
}
