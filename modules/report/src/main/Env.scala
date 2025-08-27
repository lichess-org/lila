package lila.report

import akka.actor.*
import com.softwaremill.macwire.*

import lila.core.config.*

@Module
final class Env(
    db: lila.db.Db,
    isOnline: lila.core.socket.IsOnline,
    userApi: lila.core.user.UserApi,
    lightUserAsync: lila.core.LightUser.Getter,
    gameApi: lila.core.game.GameApi,
    gameRepo: lila.core.game.GameRepo,
    securityApi: lila.core.security.SecurityApi,
    playbansOf: => lila.core.playban.BansOf,
    ircApi: lila.core.irc.IrcApi,
    settingStore: lila.memo.SettingStore.Builder,
    cacheApi: lila.memo.CacheApi,
    appConfig: play.api.Configuration,
    ws: play.api.libs.ws.StandaloneWSClient
)(using Executor, NetDomain)(using scheduler: Scheduler):

  private def lazyPlaybansOf = () => playbansOf

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
  private lazy val snoozer = new lila.memo.Snoozer[Report.SnoozeKey](cacheApi)

  private val automod = Automod(ws, appConfig)
  lazy val api = wire[ReportApi]

  lazy val modFilters = new ModReportFilter

  scheduler.scheduleWithFixedDelay(1.minute, 1.minute): () =>
    api.inquiries.expire

  lila.common.Bus.sub[lila.core.playban.Playban]:
    case lila.core.playban.Playban(userId, mins, _) => api.maybeAutoPlaybanReport(userId, mins)
