package lila.mod

import akka.actor._
import com.typesafe.config.Config

import lila.db.Types.Coll
import lila.security.{ Firewall, UserSpy }

final class Env(
    config: Config,
    db: lila.db.Env,
    system: ActorSystem,
    firewall: Firewall,
    userSpy: String => Fu[UserSpy]) {

  private val CollectionPlayerAssessment = config getString "collection.crossref"
  private val CollectionResult = config getString "collection.result"
  private val CollectionBoosting = config getString "collection.boosting"
  private val CollectionModlog = config getString "collection.modlog"
  private val ActorName = config getString "actor.name"

  private[mod] lazy val modlogColl = db(CollectionModlog)

  lazy val logApi = new ModlogApi

  lazy val assessApi = new AssessApi(db(CollectionPlayerAssessment), db(CollectionResult), logApi)

  lazy val api = new ModApi(
    logApi = logApi,
    userSpy = userSpy,
    firewall = firewall,
    lilaBus = system.lilaBus)

  private lazy val boosting = new BoostingApi(
    modApi = api,
    collBoosting = db(CollectionBoosting))

  // api actor
  private val actorApi = system.actorOf(Props(new Actor {
    def receive = {
      case lila.hub.actorApi.mod.MarkCheater(userId) => api autoAdjust userId
      case lila.analyse.actorApi.AnalysisReady(game, analysis) =>
        assessApi.onAnalysisReady(game, analysis)
      case lila.game.actorApi.FinishGame(game, whiteUserOption, blackUserOption) =>
        (whiteUserOption |@| blackUserOption) apply {
          case (whiteUser, blackUser) => boosting.check(game, whiteUser, blackUser)
        }
    }
  }), name = ActorName)
  system.lilaBus.subscribe(actorApi, 'finishGame)
}

object Env {

  lazy val current = "[boot] mod" describes new Env(
    config = lila.common.PlayApp loadConfig "mod",
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system,
    firewall = lila.security.Env.current.firewall,
    userSpy = lila.security.Env.current.userSpy)
}
