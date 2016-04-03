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

  lazy val api = new ReportApi(reportColl)

  // api actor
  system.actorOf(Props(new Actor {
    def receive = {
      case lila.hub.actorApi.report.Cheater(userId, text) =>
        api.autoCheatReport(userId, text)
      case lila.hub.actorApi.report.Clean(userId) =>
        api.clean(userId)
      case lila.hub.actorApi.report.Check(userId) =>
        api.autoProcess(userId)
      case lila.hub.actorApi.report.MarkCheater(userId, by) =>
        api.processEngine(userId, by)
      case lila.hub.actorApi.report.MarkTroll(userId, by) =>
        api.processTroll(userId, by)
      case lila.hub.actorApi.report.Shutup(userId, text) =>
        api.autoInsultReport(userId, text)
      case lila.hub.actorApi.report.Booster(userId, accomplice) =>
        api.autoBoostReport(userId, accomplice)
    }
  }), name = ActorName)

  lazy val reportColl = db(CollectionReport)
}

object Env {

  lazy val current = "report" boot new Env(
    config = lila.common.PlayApp loadConfig "report",
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system,
    hub = lila.hub.Env.current)
}
