package lila.app

import akka.actor._
import com.softwaremill.macwire._
import play.api.libs.ws.StandaloneWSClient
import play.api.mvc.{ ControllerComponents, SessionCookieBaker }
import play.api.{ Configuration, Environment }
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

import lila.common.config._
import lila.common.{ Bus, Strings, UserIds }
import lila.memo.SettingStore.Strings._
import lila.memo.SettingStore.UserIds._

final class Env(
    val config: Configuration,
    val imageRepo: lila.db.ImageRepo,
    val api: lila.api.Env,
    val user: lila.user.Env,
    val security: lila.security.Env,
    val hub: lila.hub.Env,
    val socket: lila.socket.Env,
    val memo: lila.memo.Env,
    val msg: lila.msg.Env,
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
    val appeal: lila.appeal.Env,
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
    val clas: lila.clas.Env,
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
    val swiss: lila.swiss.Env,
    val lilaCookie: lila.common.LilaCookie,
    val net: NetConfig,
    val controllerComponents: ControllerComponents
)(implicit
    val system: ActorSystem,
    val executionContext: ExecutionContext,
    val mode: play.api.Mode
) {

  val explorerEndpoint  = config.get[String]("explorer.endpoint")
  val tablebaseEndpoint = config.get[String]("explorer.tablebase.endpoint")

  val appVersionDate    = config.getOptional[String]("app.version.date")
  val appVersionCommit  = config.getOptional[String]("app.version.commit")
  val appVersionMessage = config.getOptional[String]("app.version.message")

  lazy val apiTimelineSetting = memo.settingStore[Int](
    "apiTimelineEntries",
    default = 10,
    text = "API timeline entries to serve".some
  )
  lazy val noDelaySecretSetting = memo.settingStore[Strings](
    "noDelaySecrets",
    default = Strings(Nil),
    text =
      "Secret tokens that allows fetching ongoing games without the 3-moves delay. Separated by commas.".some
  )
  lazy val featuredTeamsSetting = memo.settingStore[Strings](
    "featuredTeams",
    default = Strings(Nil),
    text = "Team IDs that always get their tournaments visible on /tournament. Separated by commas.".some
  )
  lazy val prizeTournamentMakers = memo.settingStore[UserIds](
    "prizeTournamentMakers ",
    default = UserIds(Nil),
    text =
      "User IDs who can make prize tournaments (arena & swiss) without a warning. Separated by commas.".some
  )

  lazy val preloader     = wire[mashup.Preload]
  lazy val socialInfo    = wire[mashup.UserInfo.SocialApi]
  lazy val userNbGames   = wire[mashup.UserInfo.NbGamesApi]
  lazy val userInfo      = wire[mashup.UserInfo.UserInfoApi]
  lazy val teamInfo      = wire[mashup.TeamInfoApi]
  lazy val gamePaginator = wire[mashup.GameFilterMenu.PaginatorBuilder]
  lazy val pageCache     = wire[http.PageCache]

  private val tryDailyPuzzle: lila.puzzle.DailyPuzzle.Try = () =>
    Future {
      puzzle.daily.get
    }.flatMap(identity)
      .withTimeoutDefault(50.millis, none) recover { case e: Exception =>
      lila.log("preloader").warn("daily puzzle", e)
      none
    }

  def scheduler = system.scheduler

  def closeAccount(userId: lila.user.User.ID, self: Boolean): Funit =
    for {
      u <- user.repo byId userId orFail s"No such user $userId"
      badApple = u.lameOrTrollOrAlt
      playbanned <- playban.api.hasCurrentBan(u.id)
      _          <- user.repo.disable(u, keepEmail = badApple || playbanned)
      _          <- relation.api.unfollowAll(u.id)
      _          <- user.rankingApi.remove(u.id)
      teamIds    <- team.api.quitAll(u.id)
      _          <- challenge.api.removeByUserId(u.id)
      _          <- tournament.api.withdrawAll(u)
      _          <- swiss.api.withdrawAll(u, teamIds)
      _          <- plan.api.cancel(u).nevermind
      _          <- lobby.seekApi.removeByUser(u)
      _          <- security.store.closeAllSessionsOf(u.id)
      _          <- push.webSubscriptionApi.unsubscribeByUser(u)
      _          <- streamer.api.demote(u.id)
      _          <- coach.api.remove(u.id)
      reports    <- report.api.processAndGetBySuspect(lila.report.Suspect(u))
      _          <- self ?? mod.logApi.selfCloseAccount(u.id, reports)
      _ <- u.marks.troll ?? relation.api.fetchFollowing(u.id).flatMap {
        activity.write.unfollowAll(u, _)
      }
    } yield Bus.publish(lila.hub.actorApi.security.CloseAccount(u.id), "accountClose")

  Bus.subscribeFun("garbageCollect") { case lila.hub.actorApi.security.GarbageCollect(userId) =>
    // GC can be aborted by reverting the initial SB mark
    user.repo.isTroll(userId) foreach { troll =>
      if (troll) scheduler.scheduleOnce(1.second) {
        closeAccount(userId, self = false).unit
      }
    }
  }

  system.actorOf(Props(new actor.Renderer), name = config.get[String]("app.renderer.name"))
}

final class EnvBoot(
    config: Configuration,
    environment: Environment,
    controllerComponents: ControllerComponents,
    cookieBacker: SessionCookieBaker,
    shutdown: CoordinatedShutdown
)(implicit
    ec: ExecutionContext,
    system: ActorSystem,
    ws: StandaloneWSClient
) {

  implicit def scheduler   = system.scheduler
  implicit def mode        = environment.mode
  def appPath              = AppPath(environment.rootPath)
  val netConfig            = config.get[NetConfig]("net")
  def netDomain            = netConfig.domain
  def baseUrl              = netConfig.baseUrl
  implicit def idGenerator = game.idGenerator

  lazy val mainDb: lila.db.Db = mongo.blockingDb("main", config.get[String]("mongodb.uri"))
  lazy val imageRepo          = new lila.db.ImageRepo(mainDb(CollName("image")))

  // wire all the lila modules
  lazy val memo: lila.memo.Env               = wire[lila.memo.Env]
  lazy val mongo: lila.db.Env                = wire[lila.db.Env]
  lazy val user: lila.user.Env               = wire[lila.user.Env]
  lazy val security: lila.security.Env       = wire[lila.security.Env]
  lazy val hub: lila.hub.Env                 = wire[lila.hub.Env]
  lazy val socket: lila.socket.Env           = wire[lila.socket.Env]
  lazy val msg: lila.msg.Env                 = wire[lila.msg.Env]
  lazy val game: lila.game.Env               = wire[lila.game.Env]
  lazy val bookmark: lila.bookmark.Env       = wire[lila.bookmark.Env]
  lazy val search: lila.search.Env           = wire[lila.search.Env]
  lazy val gameSearch: lila.gameSearch.Env   = wire[lila.gameSearch.Env]
  lazy val timeline: lila.timeline.Env       = wire[lila.timeline.Env]
  lazy val forum: lila.forum.Env             = wire[lila.forum.Env]
  lazy val forumSearch: lila.forumSearch.Env = wire[lila.forumSearch.Env]
  lazy val team: lila.team.Env               = wire[lila.team.Env]
  lazy val teamSearch: lila.teamSearch.Env   = wire[lila.teamSearch.Env]
  lazy val analyse: lila.analyse.Env         = wire[lila.analyse.Env]
  lazy val mod: lila.mod.Env                 = wire[lila.mod.Env]
  lazy val notifyM: lila.notify.Env          = wire[lila.notify.Env]
  lazy val round: lila.round.Env             = wire[lila.round.Env]
  lazy val lobby: lila.lobby.Env             = wire[lila.lobby.Env]
  lazy val setup: lila.setup.Env             = wire[lila.setup.Env]
  lazy val importer: lila.importer.Env       = wire[lila.importer.Env]
  lazy val tournament: lila.tournament.Env   = wire[lila.tournament.Env]
  lazy val simul: lila.simul.Env             = wire[lila.simul.Env]
  lazy val relation: lila.relation.Env       = wire[lila.relation.Env]
  lazy val report: lila.report.Env           = wire[lila.report.Env]
  lazy val appeal: lila.appeal.Env           = wire[lila.appeal.Env]
  lazy val pref: lila.pref.Env               = wire[lila.pref.Env]
  lazy val chat: lila.chat.Env               = wire[lila.chat.Env]
  lazy val puzzle: lila.puzzle.Env           = wire[lila.puzzle.Env]
  lazy val coordinate: lila.coordinate.Env   = wire[lila.coordinate.Env]
  lazy val tv: lila.tv.Env                   = wire[lila.tv.Env]
  lazy val blog: lila.blog.Env               = wire[lila.blog.Env]
  lazy val history: lila.history.Env         = wire[lila.history.Env]
  lazy val video: lila.video.Env             = wire[lila.video.Env]
  lazy val playban: lila.playban.Env         = wire[lila.playban.Env]
  lazy val shutup: lila.shutup.Env           = wire[lila.shutup.Env]
  lazy val insight: lila.insight.Env         = wire[lila.insight.Env]
  lazy val push: lila.push.Env               = wire[lila.push.Env]
  lazy val perfStat: lila.perfStat.Env       = wire[lila.perfStat.Env]
  lazy val slack: lila.slack.Env             = wire[lila.slack.Env]
  lazy val challenge: lila.challenge.Env     = wire[lila.challenge.Env]
  lazy val explorer: lila.explorer.Env       = wire[lila.explorer.Env]
  lazy val fishnet: lila.fishnet.Env         = wire[lila.fishnet.Env]
  lazy val study: lila.study.Env             = wire[lila.study.Env]
  lazy val studySearch: lila.studySearch.Env = wire[lila.studySearch.Env]
  lazy val learn: lila.learn.Env             = wire[lila.learn.Env]
  lazy val plan: lila.plan.Env               = wire[lila.plan.Env]
  lazy val event: lila.event.Env             = wire[lila.event.Env]
  lazy val coach: lila.coach.Env             = wire[lila.coach.Env]
  lazy val clas: lila.clas.Env               = wire[lila.clas.Env]
  lazy val pool: lila.pool.Env               = wire[lila.pool.Env]
  lazy val practice: lila.practice.Env       = wire[lila.practice.Env]
  lazy val irwin: lila.irwin.Env             = wire[lila.irwin.Env]
  lazy val activity: lila.activity.Env       = wire[lila.activity.Env]
  lazy val relay: lila.relay.Env             = wire[lila.relay.Env]
  lazy val streamer: lila.streamer.Env       = wire[lila.streamer.Env]
  lazy val oAuth: lila.oauth.Env             = wire[lila.oauth.Env]
  lazy val bot: lila.bot.Env                 = wire[lila.bot.Env]
  lazy val evalCache: lila.evalCache.Env     = wire[lila.evalCache.Env]
  lazy val rating: lila.rating.Env           = wire[lila.rating.Env]
  lazy val swiss: lila.swiss.Env             = wire[lila.swiss.Env]
  lazy val api: lila.api.Env                 = wire[lila.api.Env]
  lazy val lilaCookie                        = wire[lila.common.LilaCookie]

  val env: lila.app.Env = {
    val c = lila.common.Chronometer.sync(wire[lila.app.Env])
    lila.log("boot").info(s"Loaded lila modules in ${c.showDuration}")
    c.result
  }

  templating.Environment setEnv env
}
