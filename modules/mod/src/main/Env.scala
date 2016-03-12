package lila.mod

import akka.actor._
import com.typesafe.config.Config

import lila.db.Types.Coll
import lila.security.{ Firewall, UserSpy }

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env,
    system: ActorSystem,
    firewall: Firewall,
    reportColl: Coll,
    lightUserApi: lila.user.LightUserApi,
    userSpy: String => Fu[UserSpy],
    securityApi: lila.security.Api,
    emailAddress: lila.security.EmailAddress) {

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

  lazy val logApi = new ModlogApi

  lazy val api = new ModApi(
    logApi = logApi,
    userSpy = userSpy,
    firewall = firewall,
    reporter = hub.actor.report,
    lightUserApi = lightUserApi,
    lilaBus = system.lilaBus)

  private lazy val boosting = new BoostingApi(
    modApi = api,
    collBoosting = db(CollectionBoosting),
    nbGamesToMark = NbGamesToMark,
    ratioGamesToMark = RatioGamesToMark)

  lazy val assessApi = new AssessApi(
    collAssessments = db(CollectionPlayerAssessment),
    logApi = logApi,
    modApi = api,
    reporter = hub.actor.report,
    fishnet = hub.actor.fishnet,
    userIdsSharingIp = securityApi.userIdsSharingIp)

  lazy val gamify = new Gamify(
    logColl = logColl,
    reportColl = reportColl,
    historyColl = db(CollectionGamingHistory))

  lazy val search = new UserSearch(
    securityApi = securityApi,
    emailAddress = emailAddress)

  // api actor
  private val actorApi = system.actorOf(Props(new Actor {
    def receive = {
      case lila.hub.actorApi.mod.MarkCheater(userId) => api autoAdjust userId
      case lila.analyse.actorApi.AnalysisReady(game, analysis) =>
        assessApi.onAnalysisReady(game, analysis)
      case lila.game.actorApi.FinishGame(game, whiteUserOption, blackUserOption) =>
        (whiteUserOption |@| blackUserOption) apply {
          case (whiteUser, blackUser) => boosting.check(game, whiteUser, blackUser) >>
            assessApi.onGameReady(game, whiteUser, blackUser)
        }
    }
  }), name = ActorName)
  system.lilaBus.subscribe(actorApi, 'finishGame, 'analysisReady)
}

object Env {

  lazy val current = "mod" boot new Env(
    config = lila.common.PlayApp loadConfig "mod",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    system = lila.common.PlayApp.system,
    firewall = lila.security.Env.current.firewall,
    reportColl = lila.report.Env.current.reportColl,
    userSpy = lila.security.Env.current.userSpy,
    lightUserApi = lila.user.Env.current.lightUserApi,
    securityApi = lila.security.Env.current.api,
    emailAddress = lila.security.Env.current.emailAddress)
}
