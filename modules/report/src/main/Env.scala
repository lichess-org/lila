package lila.report

import scala.concurrent.duration._
import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    isOnline: lila.user.User.ID => Boolean,
    noteApi: lila.user.NoteApi,
    system: ActorSystem,
    hub: lila.hub.Env,
    asyncCache: lila.memo.AsyncCache.Builder
) {

  private val CollectionReport = config getString "collection.report"
  private val ActorName = config getString "actor.name"

  lazy val forms = new DataForm(hub.actor.captcher)

  private lazy val autoAnalysis = new AutoAnalysis(
    fishnet = hub.actor.fishnet
  )

  lazy val api = new ReportApi(
    reportColl,
    autoAnalysis,
    noteApi,
    isOnline,
    asyncCache,
    system.lilaBus
  )

  lazy val modFilters = new ModReportFilter

  // api actor
  system.actorOf(Props(new Actor {
    def receive = {
      case lila.hub.actorApi.report.Cheater(userId, text) =>
        api.autoCheatReport(userId, text)
      case lila.hub.actorApi.report.Clean(userId) =>
        api.clean(userId)
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

  system.scheduler.schedule(1 minute, 1 minute) { api.inquiries.expire }

  lazy val reportColl = db(CollectionReport)
}

object Env {

  lazy val current = "report" boot new Env(
    config = lila.common.PlayApp loadConfig "report",
    db = lila.db.Env.current,
    isOnline = lila.user.Env.current.isOnline,
    noteApi = lila.user.Env.current.noteApi,
    system = lila.common.PlayApp.system,
    hub = lila.hub.Env.current,
    asyncCache = lila.memo.Env.current.asyncCache
  )
}
