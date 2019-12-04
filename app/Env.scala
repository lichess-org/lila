package lila.app

import akka.actor._
import com.softwaremill.macwire._
import play.api.{ Application, Configuration, Mode }
import play.api.mvc.SessionCookieBaker
import scala.concurrent.duration._

import lila.common.Bus
import lila.user.User

final class Env(
    val config: Configuration,
    val playApp: Application,
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
    val notifyModule: lila.notify.Env,
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
    val rating: lila.rating.Env
)(implicit system: ActorSystem) {

  Env.bootMessage()

  val isProd = playApp.mode == Mode.Prod
  val isStage = config.get[Boolean]("app.stage")

  def net = api.net

  lazy val preloader = wire[mashup.Preload]

  lazy val socialInfo = wire[mashup.UserInfo.SocialApi]

  lazy val userNbGames = wire[mashup.UserInfo.NbGamesApi]

  lazy val userInfo = wire[mashup.UserInfo.UserInfoApi]

  lazy val teamInfo = wire[mashup.TeamInfoApi]

  private lazy val cookieBacker: SessionCookieBaker = playApp.injector.instanceOf[SessionCookieBaker]

  lazy val lilaCookie = wire[lila.common.LilaCookie]

  private val tryDailyPuzzle: lila.puzzle.Daily.Try = () =>
    scala.concurrent.Future {
      puzzle.daily.get
    }.flatMap(identity).withTimeoutDefault(50 millis, none) recover {
      case e: Exception =>
        lila.log("preloader").warn("daily puzzle", e)
        none
    }

  def closeAccount(userId: lila.user.User.ID, self: Boolean): Funit = for {
    u <- user.userRepo byId userId orFail s"No such user $userId"
    goodUser <- !u.lameOrTroll ?? { !playban.api.hasCurrentBan(u.id) }
    _ <- user.userRepo.disable(u, keepEmail = !goodUser)
    _ <- !goodUser ?? relation.api.fetchFollowing(u.id) flatMap {
      activity.write.unfollowAll(u, _)
    }
    _ <- relation.api.unfollowAll(u.id)
    _ <- user.rankingApi.remove(u.id)
    _ <- team.api.quitAll(u.id)
    _ = challenge.api.removeByUserId(u.id)
    _ = tournament.api.withdrawAll(u)
    _ <- plan.api.cancel(u).nevermind
    _ <- lobby.seekApi.removeByUser(u)
    _ <- security.store.disconnect(u.id)
    _ <- push.webSubscriptionApi.unsubscribeByUser(u)
    _ <- streamer.api.demote(u.id)
    _ <- coach.api.remove(u.id)
    reports <- report.api.processAndGetBySuspect(lila.report.Suspect(u))
    _ <- self ?? mod.logApi.selfCloseAccount(u.id, reports)
  } yield {
    Bus.publish(lila.hub.actorApi.security.CloseAccount(u.id), "accountClose")
  }

  Bus.subscribeFun("garbageCollect") {
    case lila.hub.actorApi.security.GarbageCollect(userId, _) =>
      user.userRepo.isTroll(userId) foreach { troll =>
        if (troll) kill(userId) // GC can be aborted by reverting the initial SB mark
      }
  }

  private def kill(userId: User.ID): Unit =
    system.scheduler.scheduleOnce(1 second) {
      closeAccount(userId, self = false)
    }

  system.actorOf(Props(new actor.Renderer), name = config.get[String]("app.renderer.name"))

  system.scheduler.scheduleOnce(5 seconds) { slack.api.publishRestart }

  templating.Environment setEnv this
}

private object Env {

  def bootMessage(): Unit = {
    val version = System.getProperty("java.version")
    val memory = Runtime.getRuntime().maxMemory() / 1024 / 1024
    lila.log.boot.info(s"Java: $version, memory: ${memory}MB")
  }
}
