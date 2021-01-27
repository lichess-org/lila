package lila.app

import akka.actor.CoordinatedShutdown
import com.softwaremill.macwire._
import play.api._
import play.api.libs.crypto.CookieSignerProvider
import play.api.libs.ws.StandaloneWSClient
import play.api.mvc._
import play.api.mvc.request._
import play.api.routing.Router
import router.Routes
import scala.annotation.nowarn

final class AppLoader extends ApplicationLoader {
  def load(ctx: ApplicationLoader.Context): Application = new LilaComponents(ctx).application
}

final class LilaComponents(ctx: ApplicationLoader.Context) extends BuiltInComponentsFromContext(ctx) {

  // https://www.scala-lang.org/api/2.13.4/scala/concurrent/ExecutionContext%24.html#global:scala.concurrent.ExecutionContextExecutor
  implicit val ec: scala.concurrent.ExecutionContext =
    scala.concurrent.ExecutionContext.getClass
      .getDeclaredMethod("opportunistic")
      .invoke(scala.concurrent.ExecutionContext)
      .asInstanceOf[scala.concurrent.ExecutionContext]

  LoggerConfigurator(ctx.environment.classLoader).foreach {
    _.configure(ctx.environment, ctx.initialConfiguration, Map.empty)
  }

  lila.log("boot").info {
    val java             = System.getProperty("java.version")
    val mem              = Runtime.getRuntime.maxMemory() / 1024 / 1024
    val appVersionCommit = ~configuration.getOptional[String]("app.version.commit")
    val appVersionDate   = ~configuration.getOptional[String]("app.version.date")
    s"lila ${ctx.environment.mode} $appVersionCommit $appVersionDate / java $java, memory: ${mem}MB"
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

  lazy val httpFilters = Seq(wire[lila.app.http.HttpFilter])

  override lazy val httpErrorHandler =
    new lila.app.http.ErrorHandler(
      environment = ctx.environment,
      config = configuration,
      router = router,
      mainC = main,
      lobbyC = lobby
    )

  implicit def system = actorSystem

  implicit lazy val httpClient: StandaloneWSClient = {
    import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient
    import play.api.libs.ws.WSConfigParser
    import play.api.libs.ws.ahc.{ AhcConfigBuilder, AhcWSClientConfigParser, StandaloneAhcWSClient }
    new StandaloneAhcWSClient(
      new DefaultAsyncHttpClient(
        new AhcConfigBuilder(
          new AhcWSClientConfigParser(
            new WSConfigParser(configuration.underlying, environment.classLoader).parse(),
            configuration.underlying,
            environment.classLoader
          ).parse()
        ).build()
      )
    )
  }

  // dev assets
  implicit def mimeTypes       = fileMimeTypes
  lazy val devAssetsController = wire[ExternalAssets]

  lazy val shutdown = CoordinatedShutdown(system)

  lazy val boot: lila.app.EnvBoot = wire[lila.app.EnvBoot]
  lazy val env: lila.app.Env      = boot.env

  lazy val account: Account               = wire[Account]
  lazy val analyse: Analyse               = wire[Analyse]
  lazy val api: Api                       = wire[Api]
  lazy val appeal: Appeal                 = wire[Appeal]
  lazy val auth: Auth                     = wire[Auth]
  lazy val blog: Blog                     = wire[Blog]
  lazy val bookmark: Bookmark             = wire[Bookmark]
  lazy val playApi: PlayApi               = wire[PlayApi]
  lazy val challenge: Challenge           = wire[Challenge]
  lazy val coach: Coach                   = wire[Coach]
  lazy val clas: Clas                     = wire[Clas]
  lazy val coordinate: Coordinate         = wire[Coordinate]
  lazy val dasher: Dasher                 = wire[Dasher]
  lazy val dev: Dev                       = wire[Dev]
  lazy val editor: Editor                 = wire[Editor]
  lazy val event: Event                   = wire[Event]
  lazy val export: Export                 = wire[Export]
  lazy val fishnet: Fishnet               = wire[Fishnet]
  lazy val forumCateg: ForumCateg         = wire[ForumCateg]
  lazy val forumPost: ForumPost           = wire[ForumPost]
  lazy val forumTopic: ForumTopic         = wire[ForumTopic]
  lazy val game: Game                     = wire[Game]
  lazy val i18n: I18n                     = wire[I18n]
  lazy val importer: Importer             = wire[Importer]
  lazy val insight: Insight               = wire[Insight]
  lazy val irwin: Irwin                   = wire[Irwin]
  lazy val learn: Learn                   = wire[Learn]
  lazy val lobby: Lobby                   = wire[Lobby]
  lazy val main: Main                     = wire[Main]
  lazy val msg: Msg                       = wire[Msg]
  lazy val mod: Mod                       = wire[Mod]
  lazy val notifyC: Notify                = wire[Notify]
  lazy val oAuthApp: OAuthApp             = wire[OAuthApp]
  lazy val oAuthToken: OAuthToken         = wire[OAuthToken]
  lazy val options: Options               = wire[Options]
  lazy val page: Page                     = wire[Page]
  lazy val plan: Plan                     = wire[Plan]
  lazy val practice: Practice             = wire[Practice]
  lazy val pref: Pref                     = wire[Pref]
  lazy val prismic: Prismic               = wire[Prismic]
  lazy val push: Push                     = wire[Push]
  lazy val puzzle: Puzzle                 = wire[Puzzle]
  lazy val relation: Relation             = wire[Relation]
  lazy val relay: Relay                   = wire[Relay]
  lazy val report: Report                 = wire[Report]
  lazy val round: Round                   = wire[Round]
  lazy val search: Search                 = wire[Search]
  lazy val setup: Setup                   = wire[Setup]
  lazy val simul: Simul                   = wire[Simul]
  lazy val stat: Stat                     = wire[Stat]
  lazy val streamer: Streamer             = wire[Streamer]
  lazy val study: Study                   = wire[Study]
  lazy val team: Team                     = wire[Team]
  lazy val timeline: Timeline             = wire[Timeline]
  lazy val tournament: Tournament         = wire[Tournament]
  lazy val tournamentCrud: TournamentCrud = wire[TournamentCrud]
  lazy val tv: Tv                         = wire[Tv]
  lazy val user: User                     = wire[User]
  lazy val userAnalysis: UserAnalysis     = wire[UserAnalysis]
  lazy val userTournament: UserTournament = wire[UserTournament]
  lazy val video: Video                   = wire[Video]
  lazy val swiss: Swiss                   = wire[Swiss]
  lazy val dgt: DgtCtrl                   = wire[DgtCtrl]
  lazy val storm: Storm                   = wire[Storm]

  // eagerly wire up all controllers
  val router: Router = {
    @nowarn val prefix: String = "/"
    wire[Routes]
  }

  if (configuration.get[Boolean]("kamon.enabled")) {
    lila.log("boot").info("Kamon is enabled")
    kamon.Kamon.loadModules()
  }
}
