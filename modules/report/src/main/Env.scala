package lila.report

import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import scala.concurrent.duration._

import lila.common.config._

@Module
private class ReportConfig(
    @ConfigName("collection.report") val reportColl: CollName,
    @ConfigName("score.threshold") val scoreThreshold: Int,
    @ConfigName("actor.name") val actorName: String
)

@Module
final class Env(
    appConfig: Configuration,
    domain: lila.common.config.NetDomain,
    db: lila.db.Db,
    isOnline: lila.socket.IsOnline,
    userRepo: lila.user.UserRepo,
    lightUserAsync: lila.common.LightUser.Getter,
    gameRepo: lila.game.GameRepo,
    securityApi: lila.security.SecurityApi,
    userLoginsApi: lila.security.UserLoginsApi,
    playbanApi: lila.playban.PlaybanApi,
    slackApi: lila.slack.SlackApi,
    captcher: lila.hub.actors.Captcher,
    fishnet: lila.hub.actors.Fishnet,
    settingStore: lila.memo.SettingStore.Builder,
    cacheApi: lila.memo.CacheApi
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  private val config = appConfig.get[ReportConfig]("report")(AutoConfig.loader)

  private lazy val reportColl = db(config.reportColl)

  lazy val scoreThresholdsSetting = ReportThresholds makeScoreSetting settingStore

  lazy val slackScoreThresholdSetting = ReportThresholds makeSlackSetting settingStore

  private val thresholds = Thresholds(
    score = scoreThresholdsSetting.get _,
    slack = slackScoreThresholdSetting.get _
  )

  lazy val forms = wire[ReportForm]

  private lazy val autoAnalysis = wire[AutoAnalysis]

  lazy val api = wire[ReportApi]

  lazy val modFilters = new ModReportFilter

  // api actor
  system.actorOf(
    Props(new Actor {
      def receive = {
        case lila.hub.actorApi.report.Cheater(userId, text) =>
          api.autoCheatReport(userId, text).unit
        case lila.hub.actorApi.report.Shutup(userId, text) =>
          api.autoInsultReport(userId, text).unit
        case lila.hub.actorApi.report.Booster(winnerId, loserId) =>
          api.autoBoostReport(winnerId, loserId).unit
      }
    }),
    name = config.actorName
  )

  lila.common.Bus.subscribeFun("playban", "autoFlag") {
    case lila.hub.actorApi.playban.Playban(userId, _) => api.maybeAutoPlaybanReport(userId).unit
    case lila.hub.actorApi.report.AutoFlag(suspectId, resource, text) =>
      api.autoCommFlag(SuspectId(suspectId), resource, text).unit
  }

  system.scheduler.scheduleWithFixedDelay(1 minute, 1 minute) { () =>
    api.inquiries.expire.unit
  }
}
