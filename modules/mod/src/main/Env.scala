package lila.mod

import akka.actor.*
import com.softwaremill.macwire.*

import lila.core.config.*
import lila.core.report.SuspectId
import lila.user.{ Me, User }

@Module
final class Env(
    db: lila.db.Db,
    perfStat: lila.perfStat.PerfStatApi,
    settingStore: lila.memo.SettingStore.Builder,
    reportApi: lila.report.ReportApi,
    lightUserApi: lila.user.LightUserApi,
    tournamentApi: lila.core.tournament.TournamentApi,
    swissFeature: lila.core.swiss.SwissFeatureApi,
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
    ircApi: lila.core.irc.IrcApi,
    msgApi: lila.core.msg.MsgApi
)(using Executor, Scheduler, lila.core.i18n.Translator):
  private lazy val logRepo        = ModlogRepo(db(CollName("modlog")))
  private lazy val assessmentRepo = AssessmentRepo(db(CollName("player_assessment")))
  private lazy val historyRepo    = HistoryRepo(db(CollName("mod_gaming_history")))
  private lazy val queueStatsRepo = ModQueueStatsRepo(db(CollName("mod_queue_stat")))

  lazy val presets = wire[ModPresetsApi]

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

  lazy val search = wire[ModUserSearch]

  lazy val inquiryApi = wire[InquiryApi]

  lazy val stream = wire[ModStream]

  lazy val ipRender = wire[IpRender]

  private lazy val sandbagWatch = wire[SandbagWatch]

  lila.common.Bus.subscribeFuns(
    "finishGame" -> {
      case lila.game.actorApi.FinishGame(game, users) if !game.aborted =>
        users
          .map(_.filter(_.enabled.yes).map(_.only(game.perfType)))
          .mapN: (whiteUser, blackUser) =>
            sandbagWatch(game)
            assessApi.onGameReady(game, whiteUser, blackUser)
        if game.status == chess.Status.Cheat then
          game.loserUserId.foreach: userId =>
            logApi.cheatDetectedAndCount(userId, game.id).flatMap { count =>
              (count >= 3).so {
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
      assessApi.onAnalysisReady(game, analysis)
    },
    "deletePublicChats" -> { case lila.core.actorApi.security.DeletePublicChats(userId) =>
      publicChat.deleteAll(userId)
    },
    "autoWarning" -> { case lila.core.mod.AutoWarning(userId, subject) =>
      logApi.modMessage(userId, subject)(using User.lichessIdAsMe)
    },
    "selfReportMark" -> { case lila.core.mod.SelfReportMark(suspectId, name) =>
      api.autoMark(SuspectId(suspectId), s"Self report: ${name}")(using User.lichessIdAsMe)
    },
    "chatTimeout" -> { case lila.core.mod.ChatTimeout(mod, user, reason, text) =>
      logApi.chatTimeout(user, reason, text)(using mod.into(Me.Id))
    },
    "loginWithWeakPassword"    -> { case u: User => logApi.loginWithWeakPassword(u.id) },
    "loginWithBlankedPassword" -> { case u: User => logApi.loginWithBlankedPassword(u.id) },
    "team" -> {
      case t: lila.core.team.TeamUpdate =>
        logApi.teamEdit(t.team.userId, t.team.name)(using t.me)
      case t: lila.core.team.KickFromTeam =>
        logApi.teamKick(t.userId, t.teamName)(using t.me)
    },
    "forum" -> { case p: lila.core.forum.RemovePost =>
      if p.asAdmin
      then logApi.deletePost(p.by, text = p.text.take(200))(using p.me)
      else
        logger.info:
          s"${p.me} deletes post ${p.id} by ${p.by.so(_.value)} \"${p.text.take(200)}\""
    }
  )
