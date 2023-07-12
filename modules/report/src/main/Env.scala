package lila.report

import akka.actor.*
import com.softwaremill.macwire.*
import lila.common.autoconfig.{ *, given }
import play.api.Configuration

import lila.common.config.*

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
    userApi: lila.user.UserApi,
    lightUserAsync: lila.common.LightUser.Getter,
    gameRepo: lila.game.GameRepo,
    securityApi: lila.security.SecurityApi,
    userLoginsApi: lila.security.UserLoginsApi,
    playbanApi: lila.playban.PlaybanApi,
    ircApi: lila.irc.IrcApi,
    captcher: lila.hub.actors.Captcher,
    fishnet: lila.hub.actors.Fishnet,
    settingStore: lila.memo.SettingStore.Builder,
    cacheApi: lila.memo.CacheApi
)(using ec: Executor, system: ActorSystem, scheduler: Scheduler):

  private val config = appConfig.get[ReportConfig]("report")(AutoConfig.loader)

  private lazy val reportColl = db(config.reportColl)

  lazy val scoreThresholdsSetting = ReportThresholds makeScoreSetting settingStore

  lazy val discordScoreThresholdSetting = ReportThresholds makeDiscordSetting settingStore

  private val thresholds = Thresholds(
    score = (() => scoreThresholdsSetting.get()),
    discord = (() => discordScoreThresholdSetting.get())
  )

  lazy val forms = wire[ReportForm]

  private lazy val autoAnalysis = wire[AutoAnalysis]

  private given UserIdOf[Report.SnoozeKey] = _.snoozerId
  private lazy val snoozer                 = new lila.memo.Snoozer[Report.SnoozeKey](cacheApi)

  lazy val api = wire[ReportApi]

  lazy val modFilters = new ModReportFilter

  // api actor
  system.actorOf(
    Props(
      new Actor:
        def receive =
          case lila.hub.actorApi.report.Cheater(userId, text) =>
            api.autoCheatReport(userId, text)
          case lila.hub.actorApi.report.Shutup(userId, text, critical) =>
            api.autoCommReport(userId, text, critical)
    ),
    name = config.actorName
  )

  lila.common.Bus.subscribeFun("playban", "autoFlag"):
    case lila.hub.actorApi.playban.Playban(userId, mins, _) => api.maybeAutoPlaybanReport(userId, mins)
    case lila.hub.actorApi.report.AutoFlag(suspectId, resource, text, critical) =>
      api.autoCommFlag(SuspectId(suspectId), resource, text, critical)

  scheduler.scheduleWithFixedDelay(1 minute, 1 minute): () =>
    api.inquiries.expire
