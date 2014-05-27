package lila.report

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    system: ActorSystem,
    hub: lila.hub.Env) {

  private val CollectionReport = config getString "collection.report"
  private val ActorName = config getString "actor.name"

  lazy val forms = new DataForm(hub.actor.captcher)

  lazy val api = new ReportApi(hub.actor.evaluator)

  // api actor
  system.actorOf(Props(new Actor {
    def receive = {
      case lila.hub.actorApi.report.Cheater(userId, text) =>
        api.autoCheatReport(userId, text)
      case lila.hub.actorApi.report.Blocked(userId, blocked, followed) =>
        api.autoBlockReport(userId, blocked, followed)
      case lila.hub.actorApi.report.Check(userId) =>
        api.autoProcess(userId)
    }
  }), name = ActorName)

  private[report] lazy val reportColl = db(CollectionReport)
}

object Env {

  lazy val current = "[boot] report" describes new Env(
    config = lila.common.PlayApp loadConfig "report",
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system,
    hub = lila.hub.Env.current)
}
