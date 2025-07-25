package lila.app

import com.softwaremill.macwire.*
import play.api.libs.ws.StandaloneWSClient
import play.api.mvc.{ ControllerComponents, SessionCookieBaker }
import play.api.{ Configuration, Environment, Mode }

import lila.core.config.*
import lila.common.config.GetRelativeFile

final class Env(
    val config: Configuration,
    val controllerComponents: ControllerComponents,
    environment: Environment,
    shutdown: akka.actor.CoordinatedShutdown,
    cookieBaker: SessionCookieBaker
)(using val system: akka.actor.ActorSystem, val executor: Executor)(using
    StandaloneWSClient,
    akka.stream.Materializer
):
  val net: NetConfig = lila.web.WebConfig.netConfig(config)
  export net.baseUrl

  given mode: Mode = environment.mode
  given translator: lila.core.i18n.Translator = lila.i18n.Translator
  given scheduler: Scheduler = system.scheduler
  given RateLimit = net.rateLimit
  given NetDomain = net.domain
  val getFile: GetRelativeFile = GetRelativeFile(environment.getFile(_))

  // wire all the lila modules in the right order
  val i18n: lila.i18n.Env.type = lila.i18n.Env
  val mongo: lila.db.Env = wire[lila.db.Env]
  val memo: lila.memo.Env = wire[lila.memo.Env]
  val socket: lila.socket.Env = wire[lila.socket.Env]
  val user: lila.user.Env = wire[lila.user.Env]
  import user.flairApi.given
  val mailer: lila.mailer.Env = wire[lila.mailer.Env]
  val oAuth: lila.oauth.Env = wire[lila.oauth.Env]
  val security: lila.security.Env = wire[lila.security.Env]
  val pref: lila.pref.Env = wire[lila.pref.Env]
  val relation: lila.relation.Env = wire[lila.relation.Env]
  val title: lila.title.Env = wire[lila.title.Env]
  val game: lila.game.Env = wire[lila.game.Env]
  import game.given
  val notifyM: lila.notify.Env = wire[lila.notify.Env]
  val irc: lila.irc.Env = wire[lila.irc.Env]
  val report: lila.report.Env = wire[lila.report.Env]
  val shutup: lila.shutup.Env = wire[lila.shutup.Env]
  val chat: lila.chat.Env = wire[lila.chat.Env]
  val msg: lila.msg.Env = wire[lila.msg.Env]
  val playban: lila.playban.Env = wire[lila.playban.Env]
  val evalCache: lila.evalCache.Env = wire[lila.evalCache.Env]
  val analyse: lila.analyse.Env = wire[lila.analyse.Env]
  val fishnet: lila.fishnet.Env = wire[lila.fishnet.Env]
  val history: lila.history.Env = wire[lila.history.Env]
  val bookmark: lila.bookmark.Env = wire[lila.bookmark.Env]
  val round: lila.round.Env = wire[lila.round.Env]
  val search: lila.search.Env = wire[lila.search.Env]
  val gameSearch: lila.gameSearch.Env = wire[lila.gameSearch.Env]
  val perfStat: lila.perfStat.Env = wire[lila.perfStat.Env]
  val tournament: lila.tournament.Env = wire[lila.tournament.Env]
  val swiss: lila.swiss.Env = wire[lila.swiss.Env]
  val mod: lila.mod.Env = wire[lila.mod.Env]
  val team: lila.team.Env = wire[lila.team.Env]
  val teamSearch: lila.teamSearch.Env = wire[lila.teamSearch.Env]
  val forum: lila.forum.Env = wire[lila.forum.Env]
  val forumSearch: lila.forumSearch.Env = wire[lila.forumSearch.Env]
  val pool: lila.pool.Env = wire[lila.pool.Env]
  import pool.isClockCompatible
  val lobby: lila.lobby.Env = wire[lila.lobby.Env]
  val setup: lila.setup.Env = wire[lila.setup.Env]
  val simul: lila.simul.Env = wire[lila.simul.Env]
  val appeal: lila.appeal.Env = wire[lila.appeal.Env]
  val timeline: lila.timeline.Env = wire[lila.timeline.Env]
  val puzzle: lila.puzzle.Env = wire[lila.puzzle.Env]
  val coordinate: lila.coordinate.Env = wire[lila.coordinate.Env]
  val tv: lila.tv.Env = wire[lila.tv.Env]
  val feed: lila.feed.Env = wire[lila.feed.Env]
  val video: lila.video.Env = wire[lila.video.Env]
  val insight: lila.insight.Env = wire[lila.insight.Env]
  val push: lila.push.Env = wire[lila.push.Env]
  val challenge: lila.challenge.Env = wire[lila.challenge.Env]
  val explorer: lila.explorer.Env = wire[lila.explorer.Env]
  val fide: lila.fide.Env = wire[lila.fide.Env]
  val study: lila.study.Env = wire[lila.study.Env]
  val studySearch: lila.studySearch.Env = wire[lila.studySearch.Env]
  val learn: lila.learn.Env = wire[lila.learn.Env]
  val plan: lila.plan.Env = wire[lila.plan.Env]
  val event: lila.event.Env = wire[lila.event.Env]
  val coach: lila.coach.Env = wire[lila.coach.Env]
  val clas: lila.clas.Env = wire[lila.clas.Env]
  val practice: lila.practice.Env = wire[lila.practice.Env]
  val irwin: lila.irwin.Env = wire[lila.irwin.Env]
  val ublog: lila.ublog.Env = wire[lila.ublog.Env]
  val activity: lila.activity.Env = wire[lila.activity.Env]
  val relay: lila.relay.Env = wire[lila.relay.Env]
  val streamer: lila.streamer.Env = wire[lila.streamer.Env]
  val bot: lila.bot.Env = wire[lila.bot.Env]
  val storm: lila.storm.Env = wire[lila.storm.Env]
  val racer: lila.racer.Env = wire[lila.racer.Env]
  val jsBot: lila.jsBot.Env = wire[lila.jsBot.Env]
  val opening: lila.opening.Env = wire[lila.opening.Env]
  val tutor: lila.tutor.Env = wire[lila.tutor.Env]
  val recap: lila.recap.Env = wire[lila.recap.Env]
  val cms: lila.cms.Env = wire[lila.cms.Env]
  val web: lila.web.Env = wire[lila.web.Env]
  val api: lila.api.Env = wire[lila.api.Env]

  val preloader = wire[mashup.Preload]
  val socialInfo = wire[mashup.UserInfo.SocialApi]
  val userNbGames = wire[mashup.UserInfo.NbGamesApi]
  val userInfo = wire[mashup.UserInfo.UserInfoApi]
  val teamInfo = wire[mashup.TeamInfoApi]
  val gamePaginator = wire[mashup.GameFilterMenu.PaginatorBuilder]
  val pageCache = wire[http.PageCache]

  lila.common.Bus.sub[lila.tv.RenderFeaturedJs]:
    case lila.tv.RenderFeaturedJs(game, promise) =>
      promise.success(Html(views.game.mini.noCtx(Pov.naturalOrientation(game), tv = true)))
  lila.common.Bus.sub[lila.puzzle.DailyPuzzle.Render]:
    case lila.puzzle.DailyPuzzle.Render(puzzle, fen, lastMove, promise) =>
      promise.success(Html(views.puzzle.bits.daily(puzzle, fen, lastMove)))

end Env
