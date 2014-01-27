package lila.evaluation

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env,
    system: ActorSystem) {

  private val CollectionEvaluation = config getString "collection.evaluation"
  private val EvaluatorScriptPath = config getString "evaluator.script_path"
  private val ActorName = config getString "actor.name"

  lazy val evaluator = new Evaluator(
    coll = db(CollectionEvaluation),
    script = EvaluatorScriptPath,
    reporter = hub.actor.report,
    marker = hub.actor.mod)

  system.actorOf(Props(new Listener(evaluator)), name = ActorName)
}

object Env {

  lazy val current = "[boot] evaluation" describes new Env(
    config = lila.common.PlayApp loadConfig "mod",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    system = lila.common.PlayApp.system)
}
