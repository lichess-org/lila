package lila.mod

import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration

import lila.common.config._
import lila.user.User

@Module
private class ModConfig(
    @ConfigName("collection.player_assessment") val assessmentColl: CollName,
    @ConfigName("collection.modlog") val modlogColl: CollName,
    @ConfigName("collection.gaming_history") val gamingHistoryColl: CollName,
    @ConfigName("actor.name") val actorName: String,
    @ConfigName("boosting.nb_games_to_mark") val boostingNbGamesToMark: Int,
    @ConfigName("boosting.ratio_games_to_mark") val boostingRatioToMark: Int
)

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    reporter: lila.hub.actors.Report,
    fishnet: lila.hub.actors.Fishnet,
    perfStat: lila.perfStat.Env,
    settingStore: lila.memo.SettingStore.Builder,
    reportApi: lila.report.ReportApi,
    lightUserApi: lila.user.LightUserApi,
    securityApi: lila.security.SecurityApi,
    tournamentApi: lila.tournament.TournamentApi,
    gameRepo: lila.game.GameRepo,
    analysisRepo: lila.analyse.AnalysisRepo,
    userRepo: lila.user.UserRepo,
    simulEnv: lila.simul.Env,
    chatApi: lila.chat.ChatApi,
    notifyApi: lila.notify.NotifyApi,
    historyApi: lila.history.HistoryApi,
    rankingApi: lila.user.RankingApi,
    noteApi: lila.user.NoteApi,
    cacheApi: lila.memo.CacheApi,
    slackApi: lila.slack.SlackApi
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  private val config = appConfig.get[ModConfig]("mod")(AutoConfig.loader)

  private def scheduler = system.scheduler

  private lazy val logRepo        = new ModlogRepo(db(config.modlogColl))
  private lazy val assessmentRepo = new AssessmentRepo(db(config.assessmentColl))
  private lazy val historyRepo    = new HistoryRepo(db(config.gamingHistoryColl))

  lazy val logApi = wire[ModlogApi]

  lazy val impersonate = wire[ImpersonateApi]

  private lazy val notifier = wire[ModNotifier]

  private lazy val ratingRefund = wire[RatingRefund]

  lazy val publicChat = wire[PublicChat]

  lazy val api: ModApi = wire[ModApi]

  lazy val assessApi = wire[AssessApi]

  lazy val gamify = wire[Gamify]

  lazy val search = wire[UserSearch]

  lazy val inquiryApi = wire[InquiryApi]

  lazy val stream = wire[ModStream]

  lazy val presets = wire[ModPresetsApi]

  // api actor
  lila.common.Bus.subscribe(
    system.actorOf(
      Props(new Actor {
        def receive = {
          case lila.analyse.actorApi.AnalysisReady(game, analysis) =>
            assessApi.onAnalysisReady(game, analysis).unit
          case lila.game.actorApi.FinishGame(game, whiteUserOption, blackUserOption) if !game.aborted =>
            import cats.implicits._
            (whiteUserOption, blackUserOption) mapN { (whiteUser, blackUser) =>
              assessApi.onGameReady(game, whiteUser, blackUser)
            }
            if (game.status == chess.Status.Cheat)
              game.loserUserId foreach { userId =>
                logApi.cheatDetected(userId, game.id) >>
                  logApi.countRecentCheatDetected(userId) flatMap {
                    reportApi.autoCheatDetectedReport(userId, _)
                  }
              }
          case lila.hub.actorApi.mod.ChatTimeout(mod, user, reason, text) =>
            logApi.chatTimeout(mod, user, reason, text).unit
          case lila.hub.actorApi.security.GCImmediateSb(userId) =>
            reportApi getSuspect userId orFail s"No such suspect $userId" foreach { sus =>
              reportApi.getLichessMod foreach { mod =>
                api.setTroll(mod, sus, value = true)
              }
            }
          case lila.hub.actorApi.security.GarbageCollect(userId) =>
            reportApi getSuspect userId orFail s"No such suspect $userId" foreach { sus =>
              api.garbageCollect(sus) >> publicChat.delete(sus)
            }
          case lila.hub.actorApi.mod.AutoWarning(userId, subject) =>
            logApi.modMessage(User.lichessId, userId, subject).unit
        }
      }),
      name = config.actorName
    ),
    "finishGame",
    "analysisReady",
    "garbageCollect",
    "playban",
    "autoWarning"
  )
}
