package lila.app

import com.softwaremill.macwire._
import play.api._
import play.api.mvc._
import play.api.mvc.request._
import play.api.routing.Router
import play.api.libs.crypto.CookieSignerProvider
import router.Routes

final class NopeLoader extends ApplicationLoader {
  def load(ctx: ApplicationLoader.Context): Application = new NopeComponents(ctx).application
}

final class NopeComponents(ctx: ApplicationLoader.Context)
    extends BuiltInComponentsFromContext(ctx)
    with _root_.controllers.AssetsComponents
    with play.api.libs.ws.ahc.AhcWSComponents {

  LoggerConfigurator(ctx.environment.classLoader).foreach {
    _.configure(ctx.environment, ctx.initialConfiguration, Map.empty)
  }
  import _root_.controllers._

  // we want to use the legacy session cookie baker
  // for compatibility with lila-ws
  def cookieBaker = new LegacySessionCookieBaker(httpConfiguration.session, cookieSigner)

  override lazy val requestFactory: RequestFactory = {
    val cookieSigner = new CookieSignerProvider(httpConfiguration.secret).get
    new DefaultRequestFactory(
      new DefaultCookieHeaderEncoding(httpConfiguration.cookies),
      cookieBaker,
      new LegacyFlashCookieBaker(httpConfiguration.flash, httpConfiguration.secret, cookieSigner)
    )
  }

  def netConfig        = env.net
  def mode             = environment.mode
  lazy val httpFilters = Seq(wire[lila.app.http.HttpFilter])

  override lazy val httpErrorHandler = {
    def someRouter = router.some
    def mapper     = devContext.map(_.sourceMapper)
    wire[lila.app.http.ErrorHandler]
  }

  implicit def system = actorSystem
  implicit def ws     = wsClient

  // // dev assets
  implicit def mimeTypes                       = fileMimeTypes
  lazy val devAssetsController: ExternalAssets = wire[ExternalAssets]

  val boot: lila.app.EnvBoot = wire[lila.app.EnvBoot]
  val env: lila.app.Env      = boot.env

  // lazy val account: Account               = ???
  // lazy val analyse: Analyse               = ???
  // lazy val api: Api                       = ???
  // lazy val auth: Auth                     = ???
  // lazy val blog: Blog                     = ???
  // lazy val bookmark: Bookmark             = ???
  // lazy val bot: Bot                       = ???
  // lazy val challenge: Challenge           = ???
  // lazy val coach: Coach                   = ???
  // lazy val coordinate: Coordinate         = ???
  lazy val dasher: Dasher = wire[Dasher]
  // lazy val dev: Dev                       = ???
  // lazy val editor: Editor                 = ???
  // lazy val event: Event                   = ???
  // lazy val export: Export                 = ???
  // lazy val fishnet: Fishnet               = ???
  // lazy val forumCateg: ForumCateg         = ???
  // lazy val forumPost: ForumPost           = ???
  // lazy val forumTopic: ForumTopic         = ???
  // lazy val game: Game                     = ???
  // lazy val i18n: I18n                     = ???
  // lazy val importer: Importer             = ???
  // lazy val insight: Insight               = ???
  // lazy val irwin: Irwin                   = ???
  // lazy val learn: Learn                   = ???
  // lazy val lobby: Lobby                   = ???
  // lazy val main: Main                     = ???
  // lazy val message: Message               = ???
  // lazy val mod: Mod                       = ???
  // lazy val notifyC: Notify                = ???
  // lazy val oAuthApp: OAuthApp             = ???
  // lazy val oAuthToken: OAuthToken         = ???
  lazy val options: Options = wire[Options]
  // lazy val page: Page                     = ???
  // lazy val plan: Plan                     = ???
  // lazy val practice: Practice             = ???
  // lazy val pref: Pref                     = ???
  // lazy val prismic: Prismic               = ???
  // lazy val push: Push                     = ???
  // lazy val puzzle: Puzzle                 = ???
  // lazy val relation: Relation             = ???
  // lazy val relay: Relay                   = ???
  // lazy val report: Report                 = ???
  // lazy val round: Round                   = ???
  // lazy val search: Search                 = ???
  // lazy val setup: Setup                   = ???
  // lazy val simul: Simul                   = ???
  // lazy val stat: Stat                     = ???
  // lazy val streamer: Streamer             = ???
  // lazy val study: Study                   = ???
  // lazy val team: Team                     = ???
  // lazy val timeline: Timeline             = ???
  // lazy val tournament: Tournament         = ???
  // lazy val tournamentCrud: TournamentCrud = ???
  // lazy val tv: Tv                         = ???
  // lazy val user: User                     = ???
  // lazy val userAnalysis: UserAnalysis     = ???
  // lazy val userTournament: UserTournament = ???
  // lazy val video: Video                   = ???

  // eagerly wire up all controllers
  val router: Router = {
    val prefix: String = "/"
    wire[Routes]
  }

  // if (configuration.get[String]("kamon.influxdb.hostname").nonEmpty) {
  //   lila.log("boot").info("Kamon is enabled")
  //   kamon.Kamon.loadModules()
  // }
}
