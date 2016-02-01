package lila.challenge

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    system: ActorSystem,
    onStart: String => Unit,
    lightUser: String => Option[lila.common.LightUser],
    hub: lila.hub.Env,
    db: lila.db.Env,
    scheduler: lila.common.Scheduler) {

  private val settings = new {
    val CollectionChallenge = config getString "collection.challenge"
    val ActorName = config getString "actor.name"
    val MaxPerUser = config getInt "max_per_user"
  }
  import settings._

  lazy val api = new ChallengeApi(
    repo = repo,
    jsonView = jsonView,
    userRegister = hub.actor.userRegister)

  private lazy val repo = new ChallengeRepo(
    coll = db(CollectionChallenge),
    maxPerUser = MaxPerUser)

  lazy val joiner = new Joiner(onStart = onStart)

  lazy val jsonView = new JsonView(lightUser)

  private lazy val sweeper = new Sweeper(api, repo)

  {
    import scala.concurrent.duration._

    scheduler.future(3 seconds, "sweep challenges") {
      sweeper.realTime
    }
  }
}

object Env {

  lazy val current: Env = "challenge" boot new Env(
    config = lila.common.PlayApp loadConfig "challenge",
    system = lila.common.PlayApp.system,
    onStart = lila.game.Env.current.onStart,
    hub = lila.hub.Env.current,
    lightUser = lila.user.Env.current.lightUser,
    db = lila.db.Env.current,
    scheduler = lila.common.PlayApp.scheduler)
}
