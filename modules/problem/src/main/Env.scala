package lila.problem

import akka.actor.{ ActorSystem, Props }
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    system: ActorSystem) {

  private val settings = new {
    val CollectionProblem = config getString "collection.problem"
    val ApiToken = config getString "api.token"
  }
  import settings._

  lazy val api = new ProblemApi(
    coll = problemColl,
    apiToken = ApiToken)

  private[problem] lazy val problemColl = db(CollectionProblem)
}

object Env {

  lazy val current: Env = "[boot] problem" describes new Env(
    config = lila.common.PlayApp loadConfig "problem",
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system)
}
