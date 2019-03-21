package lila.mod

import akka.actor._
import com.typesafe.config.Config

import lila.security.{ Firewall, UserSpy }
import lila.user.User

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env,
    perfStat: lila.perfStat.Env,
    system: ActorSystem,
    scheduler: lila.common.Scheduler,
    firewall: Firewall,
    reportApi: lila.report.ReportApi,
    lightUserApi: lila.user.LightUserApi,
    userSpy: User => Fu[UserSpy],
    securityApi: lila.security.SecurityApi,
    tournamentApi: lila.tournament.TournamentApi,
    simulEnv: lila.simul.Env,
    chatApi: lila.chat.ChatApi,
    notifyApi: lila.notify.NotifyApi,
    historyApi: lila.history.HistoryApi,
    rankingApi: lila.user.RankingApi,
    noteApi: lila.user.NoteApi,
    asyncCache: lila.memo.AsyncCache.Builder,
    emailValidator: lila.security.EmailAddressValidator
) {

  private object settings {
    val CollectionPlayerAssessment = config getString "collection.player_assessment"
    val CollectionBoosting = config getString "collection.boosting"
    val CollectionModlog = config getString "collection.modlog"
    val CollectionGamingHistory = config getString "collection.gaming_history"
    val ActorName = config getString "actor.name"
    val NbGamesToMark = config getInt "boosting.nb_games_to_mark"
    val RatioGamesToMark = config getDouble "boosting.ratio_games_to_mark"
  }
  import settings._

  private[mod] lazy val logColl = db(CollectionModlog)

  lazy val logApi = new ModlogApi(logColl)

  private lazy val notifier = new ModNotifier(notifyApi, reportApi)

  private lazy val ratingRefund = new RatingRefund(
    scheduler = scheduler,
    notifier = notifier,
    historyApi = historyApi,
    rankingApi = rankingApi,
    wasUnengined = logApi.wasUnengined,
    perfStatter = perfStat.get _
  )

  lazy val publicChat = new PublicChat(chatApi, tournamentApi, simulEnv)

  lazy val api = new ModApi(
    logApi = logApi,
    userSpy = userSpy,
    firewall = firewall,
    reporter = hub.report,
    reportApi = reportApi,
    lightUserApi = lightUserApi,
    notifier = notifier,
    refunder = ratingRefund,
    lilaBus = system.lilaBus
  )

  private lazy val boosting = new BoostingApi(
    modApi = api,
    collBoosting = db(CollectionBoosting),
    nbGamesToMark = NbGamesToMark,
    ratioGamesToMark = RatioGamesToMark
  )

  lazy val assessApi = new AssessApi(
    collAssessments = db(CollectionPlayerAssessment),
    logApi = logApi,
    modApi = api,
    reporter = hub.report,
    fishnet = hub.fishnet
  )

  lazy val gamify = new Gamify(
    logColl = logColl,
    reportApi = reportApi,
    asyncCache = asyncCache,
    historyColl = db(CollectionGamingHistory)
  )

  lazy val search = lila.user.UserRepo.withColl { userColl =>
    new UserSearch(
      securityApi = securityApi,
      emailValidator = emailValidator,
      userColl = userColl
    )
  }

  lazy val inquiryApi = new InquiryApi(reportApi, noteApi, logApi)

  lazy val stream = new ModStream(system)

  // api actor
  system.lilaBus.subscribe(system.actorOf(Props(new Actor {
    def receive = {
      case lila.analyse.actorApi.AnalysisReady(game, analysis) =>
        assessApi.onAnalysisReady(game, analysis)
      case lila.game.actorApi.FinishGame(game, whiteUserOption, blackUserOption) if !game.aborted =>
        (whiteUserOption |@| blackUserOption) apply {
          case (whiteUser, blackUser) => boosting.check(game, whiteUser, blackUser) >>
            assessApi.onGameReady(game, whiteUser, blackUser)
        }
        if (game.status == chess.Status.Cheat)
          game.loserUserId foreach { logApi.cheatDetected(_, game.id) }
      case lila.hub.actorApi.mod.ChatTimeout(mod, user, reason) => logApi.chatTimeout(mod, user, reason)
      case lila.hub.actorApi.security.GCImmediateSb(userId) =>
        reportApi getSuspect userId flatten s"No such suspect $userId" flatMap { sus =>
          reportApi.getLichessMod map { mod =>
            api.setTroll(mod, sus, true)
          }
        }
      case lila.hub.actorApi.security.GarbageCollect(userId, ipBan) =>
        reportApi getSuspect userId flatten s"No such suspect $userId" flatMap { sus =>
          api.garbageCollect(sus, ipBan) >> publicChat.delete(sus)
        }
    }
  }), name = ActorName), 'finishGame, 'analysisReady, 'garbageCollect)
}

object Env {

  lazy val current = "mod" boot new Env(
    config = lila.common.PlayApp loadConfig "mod",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    perfStat = lila.perfStat.Env.current,
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler,
    firewall = lila.security.Env.current.firewall,
    reportApi = lila.report.Env.current.api,
    userSpy = lila.security.Env.current.userSpy,
    lightUserApi = lila.user.Env.current.lightUserApi,
    securityApi = lila.security.Env.current.api,
    tournamentApi = lila.tournament.Env.current.api,
    simulEnv = lila.simul.Env.current,
    chatApi = lila.chat.Env.current.api,
    notifyApi = lila.notify.Env.current.api,
    historyApi = lila.history.Env.current.api,
    rankingApi = lila.user.Env.current.rankingApi,
    noteApi = lila.user.Env.current.noteApi,
    asyncCache = lila.memo.Env.current.asyncCache,
    emailValidator = lila.security.Env.current.emailAddressValidator
  )
}
