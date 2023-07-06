package lila.mod

import akka.actor.*
import com.softwaremill.macwire.*

import lila.common.config.*
import lila.user.{ User, Me }
import lila.report.{ ModId, SuspectId }

@Module
final class Env(
    db: lila.db.Db,
    reporter: lila.hub.actors.Report,
    fishnet: lila.hub.actors.Fishnet,
    perfStat: lila.perfStat.PerfStatApi,
    settingStore: lila.memo.SettingStore.Builder,
    reportApi: lila.report.ReportApi,
    lightUserApi: lila.user.LightUserApi,
    securityApi: lila.security.SecurityApi,
    tournamentApi: lila.tournament.TournamentApi,
    swissFeature: lila.swiss.SwissFeature,
    gameRepo: lila.game.GameRepo,
    analysisRepo: lila.analyse.AnalysisRepo,
    userRepo: lila.user.UserRepo,
    perfsRepo: lila.user.UserPerfsRepo,
    userApi: lila.user.UserApi,
    chatApi: lila.chat.ChatApi,
    notifyApi: lila.notify.NotifyApi,
    historyApi: lila.history.HistoryApi,
    rankingApi: lila.user.RankingApi,
    noteApi: lila.user.NoteApi,
    cacheApi: lila.memo.CacheApi,
    ircApi: lila.irc.IrcApi,
    msgApi: lila.msg.MsgApi
)(using
    ec: Executor,
    scheduler: Scheduler
):
  private lazy val logRepo        = ModlogRepo(db(CollName("modlog")))
  private lazy val assessmentRepo = AssessmentRepo(db(CollName("player_assessment")))
  private lazy val historyRepo    = HistoryRepo(db(CollName("mod_gaming_history")))
  private lazy val queueStatsRepo = ModQueueStatsRepo(db(CollName("mod_queue_stat")))

  lazy val logApi = wire[ModlogApi]

  lazy val impersonate = wire[ImpersonateApi]

  private lazy val notifier = wire[ModNotifier]

  private lazy val ratingRefund = wire[RatingRefund]

  lazy val publicChat = wire[PublicChat]

  lazy val api: ModApi = wire[ModApi]

  lazy val assessApi = wire[AssessApi]

  lazy val gamify = wire[Gamify]

  lazy val activity = wire[ModActivity]

  lazy val queueStats = wire[ModQueueStats]

  lazy val search = wire[UserSearch]

  lazy val inquiryApi = wire[InquiryApi]

  lazy val stream = wire[ModStream]

  lazy val presets = wire[ModPresetsApi]

  lazy val ipRender = wire[IpRender]

  private lazy val sandbagWatch = wire[SandbagWatch]

  lila.common.Bus.subscribeFuns(
    "finishGame" -> {
      case lila.game.actorApi.FinishGame(game, whiteUserOption, blackUserOption) if !game.aborted =>
        def userPerf = (u: Option[User.WithPerfs]) => u.filter(_.enabled.yes).map(_.only(game.perfType))
        (userPerf(whiteUserOption), userPerf(blackUserOption)) mapN { (whiteUser, blackUser) =>
          sandbagWatch(game)
          assessApi.onGameReady(game, whiteUser, blackUser)
        }
        if game.status == chess.Status.Cheat then
          game.loserUserId.foreach: userId =>
            logApi.cheatDetectedAndCount(userId, game.id) flatMap { count =>
              (count >= 3) so {
                if game.hasClock then
                  api.autoMark(
                    SuspectId(userId),
                    s"Cheat detected during game, ${count} times"
                  )(using User.lichessIdAsMe)
                else reportApi.autoCheatDetectedReport(userId, count)
              }
            }
    },
    "analysisReady" -> { case lila.analyse.actorApi.AnalysisReady(game, analysis) =>
      assessApi.onAnalysisReady(game, analysis).unit
    },
    "deletePublicChats" -> { case lila.hub.actorApi.security.DeletePublicChats(userId) =>
      publicChat.deleteAll(userId).unit
    },
    "autoWarning" -> { case lila.hub.actorApi.mod.AutoWarning(userId, subject) =>
      logApi.modMessage(userId, subject)(using User.lichessIdAsMe).unit
    },
    "selfReportMark" -> { case lila.hub.actorApi.mod.SelfReportMark(suspectId, name) =>
      api
        .autoMark(SuspectId(suspectId), s"Self report: ${name}")(using User.lichessIdAsMe)
        .unit
    },
    "chatTimeout" -> { case lila.hub.actorApi.mod.ChatTimeout(mod, user, reason, text) =>
      logApi.chatTimeout(user, reason, text)(using mod.into(Me.Id)).unit
    },
    "loginWithWeakPassword"    -> { case u: User => logApi.loginWithWeakPassword(u.id) },
    "loginWithBlankedPassword" -> { case u: User => logApi.loginWithBlankedPassword(u.id) }
  )
