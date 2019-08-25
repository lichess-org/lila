package lidraughts.mod

import akka.actor._
import com.typesafe.config.Config

import lidraughts.security.{ Firewall, UserSpy }
import lidraughts.user.User

final class Env(
    config: Config,
    db: lidraughts.db.Env,
    hub: lidraughts.hub.Env,
    perfStat: lidraughts.perfStat.Env,
    system: ActorSystem,
    scheduler: lidraughts.common.Scheduler,
    firewall: Firewall,
    reportApi: lidraughts.report.ReportApi,
    lightUserApi: lidraughts.user.LightUserApi,
    userSpy: User => Fu[UserSpy],
    securityApi: lidraughts.security.SecurityApi,
    tournamentApi: lidraughts.tournament.TournamentApi,
    simulEnv: lidraughts.simul.Env,
    chatApi: lidraughts.chat.ChatApi,
    notifyApi: lidraughts.notify.NotifyApi,
    historyApi: lidraughts.history.HistoryApi,
    rankingApi: lidraughts.user.RankingApi,
    relationApi: lidraughts.relation.RelationApi,
    noteApi: lidraughts.user.NoteApi,
    userJson: lidraughts.user.JsonView,
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    emailValidator: lidraughts.security.EmailAddressValidator
) {

  private object settings {
    val CollectionPlayerAssessment = config getString "collection.player_assessment"
    val CollectionBoosting = config getString "collection.boosting"
    val CollectionModlog = config getString "collection.modlog"
    val CollectionGamingHistory = config getString "collection.gaming_history"
    val CollectionCheatList = config getString "collection.cheat_list"
    val ActorName = config getString "actor.name"
    val NbGamesToMark = config getInt "boosting.nb_games_to_mark"
    val RatioGamesToMark = config getDouble "boosting.ratio_games_to_mark"
  }
  import settings._

  val ApiKey = config getString "api.key"

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
    lidraughtsBus = system.lidraughtsBus
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
    draughtsnet = hub.draughtsnet
  )

  lazy val gamify = new Gamify(
    logColl = logColl,
    reportApi = reportApi,
    asyncCache = asyncCache,
    historyColl = db(CollectionGamingHistory)
  )

  lazy val search = lidraughts.user.UserRepo.withColl { userColl =>
    new UserSearch(
      securityApi = securityApi,
      emailValidator = emailValidator,
      userColl = userColl
    )
  }

  lazy val jsonView = new JsonView(
    assessApi = assessApi,
    relationApi = relationApi,
    reportApi = reportApi,
    userJson = userJson
  )

  lazy val inquiryApi = new InquiryApi(reportApi, noteApi, logApi)

  lazy val cheatList = new CheatList(db(CollectionCheatList))

  lazy val stream = new ModStream(system)

  // api actor
  system.lidraughtsBus.subscribe(system.actorOf(Props(new Actor {
    def receive = {
      case lidraughts.analyse.actorApi.AnalysisReady(game, analysis) =>
        assessApi.onAnalysisReady(game, analysis)
      case lidraughts.game.actorApi.FinishGame(game, whiteUserOption, blackUserOption) if !game.aborted =>
        (whiteUserOption |@| blackUserOption) apply {
          case (whiteUser, blackUser) => boosting.check(game, whiteUser, blackUser) >>
            assessApi.onGameReady(game, whiteUser, blackUser)
        }
        if (game.status == draughts.Status.Cheat)
          game.loserUserId foreach { logApi.cheatDetected(_, game.id) }
      case lidraughts.hub.actorApi.mod.ChatTimeout(mod, user, reason) => logApi.chatTimeout(mod, user, reason)
      case lidraughts.hub.actorApi.security.GCImmediateSb(userId) =>
        reportApi getSuspect userId flatten s"No such suspect $userId" flatMap { sus =>
          reportApi.getLidraughtsMod map { mod =>
            api.setTroll(mod, sus, true)
          }
        }
      case lidraughts.hub.actorApi.security.GarbageCollect(userId, ipBan) =>
        reportApi getSuspect userId flatten s"No such suspect $userId" flatMap { sus =>
          api.garbageCollect(sus, ipBan) >> publicChat.delete(sus)
        }
      case lidraughts.hub.actorApi.mod.AutoWarning(userId, subject) =>
        logApi.modMessage(User.lidraughtsId, userId, subject)
    }
  }), name = ActorName), 'finishGame, 'analysisReady, 'garbageCollect, 'playban, 'autoWarning)
}

object Env {

  lazy val current = "mod" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "mod",
    db = lidraughts.db.Env.current,
    hub = lidraughts.hub.Env.current,
    perfStat = lidraughts.perfStat.Env.current,
    system = lidraughts.common.PlayApp.system,
    scheduler = lidraughts.common.PlayApp.scheduler,
    firewall = lidraughts.security.Env.current.firewall,
    reportApi = lidraughts.report.Env.current.api,
    userSpy = lidraughts.security.Env.current.userSpy,
    lightUserApi = lidraughts.user.Env.current.lightUserApi,
    securityApi = lidraughts.security.Env.current.api,
    tournamentApi = lidraughts.tournament.Env.current.api,
    simulEnv = lidraughts.simul.Env.current,
    chatApi = lidraughts.chat.Env.current.api,
    notifyApi = lidraughts.notify.Env.current.api,
    historyApi = lidraughts.history.Env.current.api,
    rankingApi = lidraughts.user.Env.current.rankingApi,
    relationApi = lidraughts.relation.Env.current.api,
    noteApi = lidraughts.user.Env.current.noteApi,
    userJson = lidraughts.user.Env.current.jsonView,
    asyncCache = lidraughts.memo.Env.current.asyncCache,
    emailValidator = lidraughts.security.Env.current.emailAddressValidator
  )
}
