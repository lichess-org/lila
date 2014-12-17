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
    val ApiToken = config getString "api.token"
  }
  import settings._

  lazy val api = new OpeningApi(
    openingColl = openingColl,
    attemptColl = attemptColl,
    apiToken = ApiToken)

  private[opening] lazy val openingColl = db(CollectionOpening)
  private[opening] lazy val attemptColl = db(CollectionAttempt)
}

object Env {

  lazy val current: Env = "[boot] opening" describes new Env(
    config = lila.common.PlayApp loadConfig "opening",
    db = lila.db.Env.current)
}
