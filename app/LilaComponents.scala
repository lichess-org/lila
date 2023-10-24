package lila.app

import akka.actor.ActorSystem
import com.softwaremill.macwire.*
import play.api.{ Environment, Configuration, BuiltInComponents }
import play.api.http.HttpRequestHandler
import play.api.libs.crypto.DefaultCookieSigner
import play.api.libs.ws.StandaloneWSClient
import play.api.mvc.*
import play.api.mvc.request.*
import play.api.routing.Router
import play.api.http.FileMimeTypes
import play.api.inject.ApplicationLifecycle

final class LilaComponents(
    val environment: Environment,
    val applicationLifecycle: ApplicationLifecycle,
    val configuration: Configuration
) extends BuiltInComponents:

  val controllerComponents: ControllerComponents = DefaultControllerComponents(
    defaultActionBuilder,
    playBodyParsers,
    fileMimeTypes,
    executionContext
  )

  // https://www.scala-lang.org/api/2.13.4/Executor%24.html#global:Executor
  given executor: Executor = lila.Lila.defaultExecutor

  lila.log("boot").info {
    val java             = System.getProperty("java.version")
    val mem              = Runtime.getRuntime.maxMemory() / 1024 / 1024
    val appVersionCommit = ~configuration.getOptional[String]("app.version.commit")
    val appVersionDate   = ~configuration.getOptional[String]("app.version.date")
    s"lila ${environment.mode} $appVersionCommit $appVersionDate / java $java, memory: ${mem}MB"
  }

  import _root_.controllers.*

  // we want to use the legacy session cookie baker
  // for compatibility with lila-ws
  lazy val cookieBaker = LegacySessionCookieBaker(httpConfiguration.session, cookieSigner)

  override lazy val requestFactory: RequestFactory =
    val cookieSigner = DefaultCookieSigner(httpConfiguration.secret)
    DefaultRequestFactory(
      DefaultCookieHeaderEncoding(httpConfiguration.cookies),
      cookieBaker,
      LegacyFlashCookieBaker(httpConfiguration.flash, httpConfiguration.secret, cookieSigner)
    )

  lazy val httpFilters = Seq(wire[lila.app.http.HttpFilter])

  override lazy val httpErrorHandler =
    lila.app.http.ErrorHandler(
      environment = environment,
      config = configuration,
      router = router,
      mainC = main,
      lobbyC = lobby
    )

  override lazy val httpRequestHandler: HttpRequestHandler =
    lila.app.http.HttpRequestHandler(
      router,
      httpErrorHandler,
      httpConfiguration,
      httpFilters,
      controllerComponents
    )

  given ActorSystem = actorSystem

  given StandaloneWSClient =
    import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient
    import play.api.libs.ws.WSConfigParser
    import play.api.libs.ws.ahc.{ AhcConfigBuilder, AhcWSClientConfigParser, StandaloneAhcWSClient }
    new StandaloneAhcWSClient(
      DefaultAsyncHttpClient:
        AhcConfigBuilder(
          AhcWSClientConfigParser(
            WSConfigParser(configuration.underlying, environment.classLoader).parse(),
            configuration.underlying,
            environment.classLoader
          ).parse()
        ).modifyUnderlying(_.setIoThreadsCount(8)).build()
    )

  // dev assets
  given FileMimeTypes          = fileMimeTypes
  lazy val devAssetsController = wire[ExternalAssets]

  lazy val boot: lila.app.EnvBoot = wire[lila.app.EnvBoot]
  lazy val env: lila.app.Env      = boot.env

  lazy val account: Account               = wire[Account]
  lazy val analyse: Analyse               = wire[Analyse]
  lazy val api: Api                       = wire[Api]
  lazy val appealC: appeal.Appeal         = wire[appeal.Appeal]
  lazy val auth: Auth                     = wire[Auth]
  lazy val blog: Blog                     = wire[Blog]
  lazy val playApi: PlayApi               = wire[PlayApi]
  lazy val challenge: Challenge           = wire[Challenge]
  lazy val coach: Coach                   = wire[Coach]
  lazy val clasC: clas.Clas               = wire[clas.Clas]
  lazy val coordinate: Coordinate         = wire[Coordinate]
  lazy val dasher: Dasher                 = wire[Dasher]
  lazy val dev: Dev                       = wire[Dev]
  lazy val editor: Editor                 = wire[Editor]
  lazy val event: Event                   = wire[Event]
  lazy val `export`: Export               = wire[Export]
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
  lazy val gameMod: GameMod               = wire[GameMod]
  lazy val notifyC: Notify                = wire[Notify]
  lazy val oAuth: OAuth                   = wire[OAuth]
  lazy val oAuthToken: OAuthToken         = wire[OAuthToken]
  lazy val page: ContentPage              = wire[ContentPage]
  lazy val plan: Plan                     = wire[Plan]
  lazy val practice: Practice             = wire[Practice]
  lazy val pref: Pref                     = wire[Pref]
  lazy val prismic: Prismic               = wire[Prismic]
  lazy val push: Push                     = wire[Push]
  lazy val puzzle: Puzzle                 = wire[Puzzle]
  lazy val relation: Relation             = wire[Relation]
  lazy val relay: RelayRound              = wire[RelayRound]
  lazy val relayTour: RelayTour           = wire[RelayTour]
  lazy val reportC: report.Report         = wire[report.Report]
  lazy val round: Round                   = wire[Round]
  lazy val search: Search                 = wire[Search]
  lazy val setup: Setup                   = wire[Setup]
  lazy val simul: Simul                   = wire[Simul]
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
  lazy val racer: Racer                   = wire[Racer]
  lazy val ublog: Ublog                   = wire[Ublog]
  lazy val tutor: Tutor                   = wire[Tutor]
  lazy val bulkPairing: BulkPairing       = wire[BulkPairing]
  lazy val opening: Opening               = wire[Opening]

  // eagerly wire up all controllers
  private val appealRouter: _root_.router.appeal.Routes = wire[_root_.router.appeal.Routes]
  private val reportRouter: _root_.router.report.Routes = wire[_root_.router.report.Routes]
  private val clasRouter: _root_.router.clas.Routes     = wire[_root_.router.clas.Routes]
  val router: Router                                    = wire[_root_.router.router.Routes]

  if configuration.get[Boolean]("kamon.enabled") then
    lila.log("boot").info("Kamon is enabled")
    kamon.Kamon.init()
