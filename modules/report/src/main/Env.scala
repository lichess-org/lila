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

private case class Thresholds(score: () => Int, slack: () => Int)

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
    userSpyApi: lila.security.UserSpyApi,
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

  lazy val scoreThresholdSetting = settingStore[Int](
    "reportScoreThreshold",
    default = config.scoreThreshold,
    text = "Report score threshold. Reports with lower scores are concealed to moderators".some
  )

  lazy val slackScoreThresholdSetting = settingStore[Int](
    "slackScoreThreshold",
    default = 80,
    text = "Slack score threshold. Comm reports with higher scores are notified in slack".some
  )

  private val thresholds = Thresholds(
    score = scoreThresholdSetting.get _,
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
          api.autoCheatReport(userId, text)
        case lila.hub.actorApi.report.Shutup(userId, text, major) =>
          api.autoInsultReport(userId, text, major)
        case lila.hub.actorApi.report.Booster(winnerId, loserId) =>
          api.autoBoostReport(winnerId, loserId)
      }
    }),
    name = config.actorName
  )

  lila.common.Bus.subscribeFun("playban", "autoFlag") {
    case lila.hub.actorApi.playban.Playban(userId, _) => api.maybeAutoPlaybanReport(userId)
    case lila.hub.actorApi.report.AutoFlag(suspectId, resource, text) =>
      api.autoCommFlag(SuspectId(suspectId), resource, text)
  }

  system.scheduler.scheduleWithFixedDelay(1 minute, 1 minute) { () =>
    api.inquiries.expire
  }
}
