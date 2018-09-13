package lidraughts.report

import scala.concurrent.duration._
import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lidraughts.db.Env,
    isOnline: lidraughts.user.User.ID => Boolean,
    noteApi: lidraughts.user.NoteApi,
    securityApi: lidraughts.security.SecurityApi,
    system: ActorSystem,
    hub: lidraughts.hub.Env,
    settingStore: lidraughts.memo.SettingStore.Builder,
    asyncCache: lidraughts.memo.AsyncCache.Builder
) {

  private val CollectionReport = config getString "collection.report"
  private val ActorName = config getString "actor.name"
  private val ScoreThreshold = config getInt "score.threshold"

  val scoreThresholdSetting = settingStore[Int](
    "reportScoreThreshold",
    default = ScoreThreshold,
    text = "Report score threshold. Reports with lower scores are concealed to moderators".some
  )

  lazy val forms = new DataForm(hub.actor.captcher)

  private lazy val autoAnalysis = new AutoAnalysis(
    draughtsnet = hub.actor.draughtsnet,
    system = system
  )

  lazy val api = new ReportApi(
    reportColl,
    autoAnalysis,
    noteApi,
    securityApi,
    isOnline,
    asyncCache,
    system.lidraughtsBus,
    scoreThreshold = scoreThresholdSetting.get
  )

  lazy val modFilters = new ModReportFilter

  def cli = new lidraughts.common.Cli {
    def process = {
      case "report" :: "score" :: "reset" :: Nil => api.resetScores inject "done"
    }
  }

  // api actor
  system.actorOf(Props(new Actor {
    def receive = {
      case lidraughts.hub.actorApi.report.Cheater(userId, text) =>
        api.autoCheatReport(userId, text)
      case lidraughts.hub.actorApi.report.Shutup(userId, text) =>
        api.autoInsultReport(userId, text)
      case lidraughts.hub.actorApi.report.Booster(winnerId, loserId) =>
        api.autoBoostReport(winnerId, loserId)
    }
  }), name = ActorName)

  system.scheduler.schedule(1 minute, 1 minute) { api.inquiries.expire }

  lazy val reportColl = db(CollectionReport)
}

object Env {

  lazy val current = "report" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "report",
    db = lidraughts.db.Env.current,
    isOnline = lidraughts.user.Env.current.isOnline,
    noteApi = lidraughts.user.Env.current.noteApi,
    securityApi = lidraughts.security.Env.current.api,
    system = lidraughts.common.PlayApp.system,
    hub = lidraughts.hub.Env.current,
    settingStore = lidraughts.memo.Env.current.settingStore,
    asyncCache = lidraughts.memo.Env.current.asyncCache
  )
}
