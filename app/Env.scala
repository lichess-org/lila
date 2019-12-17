package lila.app

import akka.actor._
import com.softwaremill.macwire._
import play.api.inject.ApplicationLifecycle
import play.api.libs.ws.WSClient
import play.api.mvc.{ ControllerComponents, SessionCookieBaker }
import play.api.{ Configuration, Environment, Mode }
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

import lila.common.Bus
import lila.common.config._
import lila.user.User

final class Env(
    val config: Configuration,
    val mode: Mode,
    val common: lila.common.Env,
    val imageRepo: lila.db.ImageRepo,
    val api: lila.api.Env,
    val user: lila.user.Env,
    val security: lila.security.Env,
    val hub: lila.hub.Env,
    val socket: lila.socket.Env,
    val memo: lila.memo.Env,
    val message: lila.message.Env,
    val i18n: lila.i18n.Env,
    val game: lila.game.Env,
    val bookmark: lila.bookmark.Env,
    val search: lila.search.Env,
    val gameSearch: lila.gameSearch.Env,
    val timeline: lila.timeline.Env,
    val forum: lila.forum.Env,
    val forumSearch: lila.forumSearch.Env,
    val team: lila.team.Env,
    val teamSearch: lila.teamSearch.Env,
    val analyse: lila.analyse.Env,
    val mod: lila.mod.Env,
    val notifyM: lila.notify.Env,
    val round: lila.round.Env,
    val lobby: lila.lobby.Env,
    val setup: lila.setup.Env,
    val importer: lila.importer.Env,
    val tournament: lila.tournament.Env,
    val simul: lila.simul.Env,
    val relation: lila.relation.Env,
    val report: lila.report.Env,
    val pref: lila.pref.Env,
    val chat: lila.chat.Env,
    val puzzle: lila.puzzle.Env,
    val coordinate: lila.coordinate.Env,
    val tv: lila.tv.Env,
    val blog: lila.blog.Env,
    val history: lila.history.Env,
    val video: lila.video.Env,
    val playban: lila.playban.Env,
    val shutup: lila.shutup.Env,
    val insight: lila.insight.Env,
    val push: lila.push.Env,
    val perfStat: lila.perfStat.Env,
    val slack: lila.slack.Env,
    val challenge: lila.challenge.Env,
    val explorer: lila.explorer.Env,
    val fishnet: lila.fishnet.Env,
    val study: lila.study.Env,
    val studySearch: lila.studySearch.Env,
    val learn: lila.learn.Env,
    val plan: lila.plan.Env,
    val event: lila.event.Env,
    val coach: lila.coach.Env,
    val pool: lila.pool.Env,
    val practice: lila.practice.Env,
    val irwin: lila.irwin.Env,
    val activity: lila.activity.Env,
    val relay: lila.relay.Env,
    val streamer: lila.streamer.Env,
    val oAuth: lila.oauth.Env,
    val bot: lila.bot.Env,
    val evalCache: lila.evalCache.Env,
    val rating: lila.rating.Env,
    val lilaCookie: lila.common.LilaCookie,
    val controllerComponents: ControllerComponents
)(implicit val system: ActorSystem, val executionContext: ExecutionContext) {

  def isProd            = mode == Mode.Prod
  def isDev             = mode == Mode.Dev
  def isStage           = config.get[Boolean]("app.stage")
  def explorerEndpoint  = config.get[String]("explorer.endpoint")
  def tablebaseEndpoint = config.get[String]("explorer.tablebase.endpoint")

  def net = common.netConfig

  lazy val preloader: mashup.Preload                             = ???
  lazy val socialInfo: mashup.UserInfo.SocialApi                 = ???
  lazy val userNbGames: mashup.UserInfo.NbGamesApi               = ???
  lazy val userInfo: mashup.UserInfo.UserInfoApi                 = ???
  lazy val teamInfo: mashup.TeamInfoApi                          = ???
  lazy val gamePaginator: mashup.GameFilterMenu.PaginatorBuilder = ???

  private val tryDailyPuzzle: lila.puzzle.Daily.Try = ???

  def scheduler = system.scheduler

  def closeAccount(userId: lila.user.User.ID, self: Boolean): Funit = ???

  private def kill(userId: User.ID): Unit = ???

  // system.actorOf(Props(new actor.Renderer), name = config.get[String]("app.renderer.name"))

  // scheduler.scheduleOnce(5 seconds) { slack.api.publishRestart }
}

final class EnvBoot(
    config: Configuration,
    environment: Environment,
    lifecycle: ApplicationLifecycle,
    controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext, system: ActorSystem) {

  // lila.log("boot").info {
  //   val mem = Runtime.getRuntime().maxMemory() / 1024 / 1024
  //   s"lila 3 / java ${System.getProperty("java.version")}, memory: ${mem}MB"
  // }

  // implicit def scheduler   = system.scheduler
  // def appPath              = AppPath(environment.rootPath)
  // def mode                 = environment.mode
  // def baseUrl              = common.netConfig.baseUrl
  // implicit def idGenerator = game.idGenerator

  // import reactivemongo.api.MongoConnection.ParsedURI
  // import lila.db.DbConfig.uriLoader
  // lazy val mainDb: lila.db.Db           = ???
  // lazy val imageRepo: lila.db.ImageRepo = ??? //          = new lila.db.ImageRepo(mainDb(CollName("image")))

  // wire all the lila modules
  // lazy val common: lila.common.Env           = ???
  // lazy val memo: lila.memo.Env               = ???
  // lazy val mongo: lila.db.Env                = ???
  // lazy val user: lila.user.Env               = ???
  // lazy val security: lila.security.Env       = ???
  // lazy val hub: lila.hub.Env                 = ???
  // lazy val socket: lila.socket.Env           = ???
  // lazy val message: lila.message.Env         = ???
  // lazy val i18n: lila.i18n.Env               = ???
  // lazy val game: lila.game.Env               = ???
  // lazy val bookmark: lila.bookmark.Env       = ???
  // lazy val search: lila.search.Env           = ???
  // lazy val gameSearch: lila.gameSearch.Env   = ???
  // lazy val timeline: lila.timeline.Env       = ???
  // lazy val forum: lila.forum.Env             = ???
  // lazy val forumSearch: lila.forumSearch.Env = ???
  // lazy val team: lila.team.Env               = ???
  // lazy val teamSearch: lila.teamSearch.Env   = ???
  // lazy val analyse: lila.analyse.Env         = ???
  // lazy val mod: lila.mod.Env                 = ???
  // lazy val notifyM: lila.notify.Env          = ???
  // lazy val round: lila.round.Env             = ???
  // lazy val lobby: lila.lobby.Env             = ???
  // lazy val setup: lila.setup.Env             = ???
  // lazy val importer: lila.importer.Env       = ???
  // lazy val tournament: lila.tournament.Env   = ???
  // lazy val simul: lila.simul.Env             = ???
  // lazy val relation: lila.relation.Env       = ???
  // lazy val report: lila.report.Env           = ???
  // lazy val pref: lila.pref.Env               = ???
  // lazy val chat: lila.chat.Env               = ???
  // lazy val puzzle: lila.puzzle.Env           = ???
  // lazy val coordinate: lila.coordinate.Env   = ???
  // lazy val tv: lila.tv.Env                   = ???
  // lazy val blog: lila.blog.Env               = ???
  // lazy val history: lila.history.Env         = ???
  // lazy val video: lila.video.Env             = ???
  // lazy val playban: lila.playban.Env         = ???
  // lazy val shutup: lila.shutup.Env           = ???
  // lazy val insight: lila.insight.Env         = ???
  // lazy val push: lila.push.Env               = ???
  // lazy val perfStat: lila.perfStat.Env       = ???
  // lazy val slack: lila.slack.Env             = ???
  // lazy val challenge: lila.challenge.Env     = ???
  // lazy val explorer: lila.explorer.Env       = ???
  // lazy val fishnet: lila.fishnet.Env         = ???
  // lazy val study: lila.study.Env             = ???
  // lazy val studySearch: lila.studySearch.Env = ???
  // lazy val learn: lila.learn.Env             = ???
  // lazy val plan: lila.plan.Env               = ???
  // lazy val event: lila.event.Env             = ???
  // lazy val coach: lila.coach.Env             = ???
  // lazy val pool: lila.pool.Env               = ???
  // lazy val practice: lila.practice.Env       = ???
  // lazy val irwin: lila.irwin.Env             = ???
  // lazy val activity: lila.activity.Env       = ???
  // lazy val relay: lila.relay.Env             = ???
  // lazy val streamer: lila.streamer.Env       = ???
  // lazy val oAuth: lila.oauth.Env             = ???
  // lazy val bot: lila.bot.Env                 = ???
  // lazy val evalCache: lila.evalCache.Env     = ???
  // lazy val rating: lila.rating.Env           = ???
  // lazy val api: lila.api.Env                 = ???
  // lazy val cookie: lila.common.LilaCookie    = ???

  // lazy val env: lila.app.Env = ???
  // val c = lila.common.Chronometer.sync(wire[lila.app.Env])
  // lila.log("boot").info(s"Loaded lila modules in ${c.showDuration}")
  // c.result
  // }

  // templating.Environment setEnv env
}
