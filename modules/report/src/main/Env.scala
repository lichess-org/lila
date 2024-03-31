package lila.report

import akka.actor.*
import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.config.*
import lila.core.report.SuspectId

@Module
final class Env(
    domain: lila.common.config.NetDomain,
    db: lila.db.Db,
    isOnline: lila.core.socket.IsOnline,
    userRepo: lila.user.UserRepo,
    userApi: lila.user.UserApi,
    lightUserAsync: lila.common.LightUser.Getter,
    gameRepo: lila.game.GameRepo,
    securityApi: lila.security.SecurityApi,
    userLoginsApi: lila.security.UserLoginsApi,
    playbanApi: lila.playban.PlaybanApi,
    ircApi: lila.irc.IrcApi,
    captcha: lila.core.captcha.CaptchaApi,
    fishnet: lila.core.actors.Fishnet,
    settingStore: lila.memo.SettingStore.Builder,
    cacheApi: lila.memo.CacheApi
)(using Executor, ActorSystem)(using scheduler: Scheduler):

  private lazy val reportColl = db(CollName("report2"))

  lazy val scoreThresholdsSetting = ReportThresholds.makeScoreSetting(settingStore)

  lazy val discordScoreThresholdSetting = ReportThresholds.makeDiscordSetting(settingStore)

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

  scheduler.scheduleWithFixedDelay(1 minute, 1 minute): () =>
    api.inquiries.expire
