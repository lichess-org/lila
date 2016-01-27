package lila.challenge

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    system: ActorSystem,
    db: lila.db.Env) {

  private val settings = new {
    val CollectionChallenge = config getString "collection.challenge"
    val ActorName = config getString "actor.name"
    val MaxPerUser = config getInt "max_per_user"
  }
  import settings._

  lazy val api = new ChallengeApi(
    coll = db(CollectionChallenge),
    maxPerUser = MaxPerUser)
}

object Env {

  lazy val current: Env = "challenge" boot new Env(
    config = lila.common.PlayApp loadConfig "challenge",
    system = lila.common.PlayApp.system,
    db = lila.db.Env.current)
}
