package lila.mod

import akka.actor.*
import chess.ByColor
import com.softwaremill.macwire.*

import lila.common.Bus
import lila.core.config.*
import lila.core.forum.BusForum
import lila.core.report.SuspectId
import lila.rating.UserWithPerfs.only
import lila.core.mod.BoardApiMark

@Module
final class Env(
    db: lila.db.Db,
    perfStat: lila.core.perf.PerfStatApi,
    settingStore: lila.memo.SettingStore.Builder,
    reportApi: lila.report.ReportApi,
    lightUserApi: lila.user.LightUserApi,
    tournamentApi: lila.core.tournament.TournamentApi,
    swissFeature: lila.core.swiss.SwissFeatureApi,
    gameRepo: lila.game.GameRepo,
    gameApi: lila.core.game.GameApi,
    analysisRepo: lila.analyse.AnalysisRepo,
    userRepo: lila.user.UserRepo,
    userApi: lila.user.UserApi,
    perfsRepo: lila.user.UserPerfsRepo,
    chatApi: lila.chat.ChatApi,
    notifyApi: lila.core.notify.NotifyApi,
    historyApi: lila.core.history.HistoryApi,
    prefApi: lila.core.pref.PrefApi,
    rankingApi: lila.user.RankingApi,
    noteApi: lila.user.NoteApi,
    cacheApi: lila.memo.CacheApi,
    ircApi: lila.core.irc.IrcApi,
    msgApi: lila.core.msg.MsgApi
)(using Executor, Scheduler, lila.core.i18n.Translator, akka.stream.Materializer):

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

  Bus.subscribeFuns(
    "finishGame" -> {
      case lila.core.game.FinishGame(game, users) if !game.aborted =>
        users
          .map(_.filter(_.enabled.yes).map(_.only(game.perfKey)))
          .mapN: (whiteUser, blackUser) =>
            sandbagWatch(game)
            assessApi.onGameReady(game, ByColor(whiteUser, blackUser))
        if game.status == chess.Status.Cheat then
          game.loserUserId.foreach: userId =>
            logApi.cheatDetectedAndCount(userId, game.id).flatMap { count =>
              (count >= 3).so {
                if game.hasClock then
                  api.autoMark(
                    SuspectId(userId),
                    s"Cheat detected during game, ${count} times"
                  )(using UserId.lichessAsMe)
                else reportApi.autoCheatDetectedReport(userId, count)
              }
            }
    },
    "analysisReady" -> { case lila.analyse.actorApi.AnalysisReady(game, analysis) =>
      assessApi.onAnalysisReady(game, analysis)
    },
    "deletePublicChats" -> { case lila.core.security.DeletePublicChats(u) =>
      publicChat.deleteAll(u)
    },
    "autoWarning" -> { case lila.core.mod.AutoWarning(userId, subject) =>
      logApi.modMessage(userId, subject)(using UserId.lichessAsMe)
    },
    "selfReportMark" -> { case lila.core.mod.SelfReportMark(suspectId, name) =>
      api.autoMark(SuspectId(suspectId), s"Self report: ${name}")(using UserId.lichessAsMe)
    },
    "chatTimeout" -> { case lila.core.mod.ChatTimeout(mod, user, reason, text) =>
      logApi.chatTimeout(user, reason, text)(using mod.into(MyId))
    },
    "loginWithWeakPassword"    -> { case u: User => logApi.loginWithWeakPassword(u.id) },
    "loginWithBlankedPassword" -> { case u: User => logApi.loginWithBlankedPassword(u.id) },
    "team" -> {
      case t: lila.core.team.TeamUpdate if t.byMod =>
        logApi.teamEdit(t.team.userId, t.team.name)(using t.me)
      case t: lila.core.team.KickFromTeam =>
        logApi.teamKick(t.userId, t.teamName)(using t.me)
    }
  )

  Bus.sub[BusForum]:
    case p: BusForum.RemovePost =>
      if p.asAdmin
      then logApi.deletePost(p.by, text = p.text.take(200))(using p.me)
      else
        logger.info:
          s"${p.me} deletes post ${p.id} by ${p.by.so(_.value)} \"${p.text.take(200)}\""

  Bus.sub[BoardApiMark]:
    case BoardApiMark(userId, name) =>
      api.autoMark(SuspectId(userId), s"Board API: ${name}")(using UserId.lichessAsMe)
