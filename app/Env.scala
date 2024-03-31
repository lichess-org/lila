package lila.app

import akka.actor.*
import com.softwaremill.macwire.*
import play.api.libs.ws.StandaloneWSClient
import play.api.mvc.{ ControllerComponents, SessionCookieBaker }
import play.api.{ Configuration, Environment, Mode }

import lila.common.config.*
import lila.common.{ Strings, UserIds }
import lila.memo.SettingStore.Strings.given
import lila.memo.SettingStore.UserIds.given
import lila.core.i18n.Translator

final class Env(
    val config: Configuration,
    val controllerComponents: ControllerComponents,
    environment: Environment,
    shutdown: CoordinatedShutdown
)(using val system: ActorSystem, val executor: Executor)(using
    JavaExecutor,
    StandaloneWSClient,
    akka.stream.Materializer,
    SessionCookieBaker
):
  val net = config.get[NetConfig]("net")
  export net.{ domain, baseUrl, assetBaseUrlInternal }

  given Mode                   = environment.mode
  given translator: Translator = lila.i18n.Translator
  given scheduler: Scheduler   = system.scheduler

  // wire all the lila modules in the right order
  val i18n: lila.i18n.Env.type          = lila.i18n.Env
  val mongo: lila.db.Env                = wire[lila.db.Env]
  val memo: lila.memo.Env               = wire[lila.memo.Env]
  val socket: lila.socket.Env           = wire[lila.socket.Env]
  val user: lila.user.Env               = wire[lila.user.Env]
  val mailer: lila.mailer.Env           = wire[lila.mailer.Env]
  val oAuth: lila.oauth.Env             = wire[lila.oauth.Env]
  val security: lila.security.Env       = wire[lila.security.Env]
  val pref: lila.pref.Env               = wire[lila.pref.Env]
  val relation: lila.relation.Env       = wire[lila.relation.Env]
  val game: lila.game.Env               = wire[lila.game.Env]
  val notifyM: lila.notify.Env          = wire[lila.notify.Env]
  val irc: lila.irc.Env                 = wire[lila.irc.Env]
  val report: lila.report.Env           = wire[lila.report.Env]
  val shutup: lila.shutup.Env           = wire[lila.shutup.Env]
  val chat: lila.chat.Env               = wire[lila.chat.Env]
  val msg: lila.msg.Env                 = wire[lila.msg.Env]
  val playban: lila.playban.Env         = wire[lila.playban.Env]
  val evalCache: lila.evalCache.Env     = wire[lila.evalCache.Env]
  val analyse: lila.analyse.Env         = wire[lila.analyse.Env]
  val fishnet: lila.fishnet.Env         = wire[lila.fishnet.Env]
  val history: lila.history.Env         = wire[lila.history.Env]
  val round: lila.round.Env             = wire[lila.round.Env]
  val bookmark: lila.bookmark.Env       = wire[lila.bookmark.Env]
  val search: lila.search.Env           = wire[lila.search.Env]
  val gameSearch: lila.gameSearch.Env   = wire[lila.gameSearch.Env]
  val perfStat: lila.perfStat.Env       = wire[lila.perfStat.Env]
  val tournament: lila.tournament.Env   = wire[lila.tournament.Env]
  val swiss: lila.swiss.Env             = wire[lila.swiss.Env]
  val mod: lila.mod.Env                 = wire[lila.mod.Env]
  val forum: lila.forum.Env             = wire[lila.forum.Env]
  val forumSearch: lila.forumSearch.Env = wire[lila.forumSearch.Env]
  val team: lila.team.Env               = wire[lila.team.Env]
  val teamSearch: lila.teamSearch.Env   = wire[lila.teamSearch.Env]
  val pool: lila.pool.Env               = wire[lila.pool.Env]
  val lobby: lila.lobby.Env             = wire[lila.lobby.Env]
  val setup: lila.setup.Env             = wire[lila.setup.Env]
  val importer: lila.importer.Env       = wire[lila.importer.Env]
  val simul: lila.simul.Env             = wire[lila.simul.Env]
  val appeal: lila.appeal.Env           = wire[lila.appeal.Env]
  val timeline: lila.timeline.Env       = wire[lila.timeline.Env]
  val puzzle: lila.puzzle.Env           = wire[lila.puzzle.Env]
  val coordinate: lila.coordinate.Env   = wire[lila.coordinate.Env]
  val tv: lila.tv.Env                   = wire[lila.tv.Env]
  val feed: lila.feed.Env               = wire[lila.feed.Env]
  val video: lila.video.Env             = wire[lila.video.Env]
  val insight: lila.insight.Env         = wire[lila.insight.Env]
  val push: lila.push.Env               = wire[lila.push.Env]
  val challenge: lila.challenge.Env     = wire[lila.challenge.Env]
  val explorer: lila.explorer.Env       = wire[lila.explorer.Env]
  val fide: lila.fide.Env               = wire[lila.fide.Env]
  val study: lila.study.Env             = wire[lila.study.Env]
  val studySearch: lila.studySearch.Env = wire[lila.studySearch.Env]
  val learn: lila.learn.Env             = wire[lila.learn.Env]
  val plan: lila.plan.Env               = wire[lila.plan.Env]
  val event: lila.event.Env             = wire[lila.event.Env]
  val coach: lila.coach.Env             = wire[lila.coach.Env]
  val clas: lila.clas.Env               = wire[lila.clas.Env]
  val practice: lila.practice.Env       = wire[lila.practice.Env]
  val irwin: lila.irwin.Env             = wire[lila.irwin.Env]
  val ublog: lila.ublog.Env             = wire[lila.ublog.Env]
  val activity: lila.activity.Env       = wire[lila.activity.Env]
  val relay: lila.relay.Env             = wire[lila.relay.Env]
  val streamer: lila.streamer.Env       = wire[lila.streamer.Env]
  val bot: lila.bot.Env                 = wire[lila.bot.Env]
  val storm: lila.storm.Env             = wire[lila.storm.Env]
  val racer: lila.racer.Env             = wire[lila.racer.Env]
  val opening: lila.opening.Env         = wire[lila.opening.Env]
  val tutor: lila.tutor.Env             = wire[lila.tutor.Env]
  val cms: lila.cms.Env                 = wire[lila.cms.Env]
  val api: lila.api.Env                 = wire[lila.api.Env]
  val lilaCookie                        = wire[lila.common.LilaCookie]

  val explorerEndpoint       = config.get[String]("explorer.endpoint")
  val tablebaseEndpoint      = config.get[String]("explorer.tablebase_endpoint")
  val externalEngineEndpoint = config.get[String]("externalEngine.endpoint")

  val appVersionDate    = config.getOptional[String]("app.version.date")
  val appVersionCommit  = config.getOptional[String]("app.version.commit")
  val appVersionMessage = config.getOptional[String]("app.version.message")

  val apiTimelineSetting = memo.settingStore[Int](
    "apiTimelineEntries",
    default = 10,
    text = "API timeline entries to serve".some
  )
  val noDelaySecretSetting = memo.settingStore[Strings](
    "noDelaySecrets",
    default = Strings(Nil),
    text =
      "Secret tokens that allows fetching ongoing games without the 3-moves delay. Separated by commas.".some
  )
  val prizeTournamentMakers = memo.settingStore[UserIds](
    "prizeTournamentMakers",
    default = UserIds(Nil),
    text =
      "User IDs who can make prize tournaments (arena & swiss) without a warning. Separated by commas.".some
  )
  val apiExplorerGamesPerSecond = memo.settingStore[Int](
    "apiExplorerGamesPerSecond",
    default = 300,
    text = "Opening explorer games per second".some
  )
  val pieceImageExternal = memo.settingStore[Boolean](
    "pieceImageExternal",
    default = false,
    text = "Use external piece images".some
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
      .withTimeoutDefault(50.millis, none)
      .recover { case e: Exception =>
        lila.log("preloader").warn("daily puzzle", e)
        none
      }

  lila.common.Bus.subscribeFun("renderer"):
    case lila.tv.RenderFeaturedJs(game, promise) =>
      promise.success(Html(views.html.game.mini.noCtx(lila.game.Pov.naturalOrientation(game), tv = true)))
    case lila.puzzle.DailyPuzzle.Render(puzzle, fen, lastMove, promise) =>
      promise.success(Html(views.html.puzzle.bits.daily(puzzle, fen, lastMove)))

end Env
