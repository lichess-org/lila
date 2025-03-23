package lila.app

import akka.actor.ActorSystem
import com.softwaremill.macwire.*
import play.api.inject.DefaultApplicationLifecycle
import play.api.http.{ FileMimeTypes, HttpRequestHandler }
import play.api.inject.ApplicationLifecycle
import play.api.libs.crypto.DefaultCookieSigner
import play.api.libs.ws.StandaloneWSClient
import play.api.mvc.*
import play.api.mvc.request.*
import play.api.routing.Router
import play.api.{ BuiltInComponents, Configuration, Environment }

// The program entry point.
// To run with bloop:
// /path/to/bloop run lila -m lila.app.Lila -c /path/to/lila/.bloop
object Lila:

  def main(args: Array[String]): Unit =
    lila.web.PlayServer.start(args): env =>
      LilaComponents(
        env,
        DefaultApplicationLifecycle(),
        Configuration.load(env)
      ).application

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

  given executor: Executor = scala.concurrent.ExecutionContextOpportunistic

  lila
    .log("boot")
    .info:
      val appVersionCommit = ~configuration.getOptional[String]("app.version.commit")
      val appVersionDate   = ~configuration.getOptional[String]("app.version.date")
      s"lila version: $appVersionCommit $appVersionDate"

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

  given ActorSystem = actorSystem

  given StandaloneWSClient =
    import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient
    import play.api.libs.ws.WSConfigParser
    import play.api.libs.ws.ahc.{ AhcConfigBuilder, AhcWSClientConfigParser, StandaloneAhcWSClient }
    new StandaloneAhcWSClient(
      DefaultAsyncHttpClient(
        AhcConfigBuilder(
          AhcWSClientConfigParser(
            WSConfigParser(configuration.underlying, environment.classLoader).parse(),
            configuration.underlying,
            environment.classLoader
          ).parse()
        ).modifyUnderlying(_.setIoThreadsCount(8)).build()
      )
    )

  val env: lila.app.Env =
    lila.log("boot").info(s"Start loading lila modules")
    val c = lila.common.Chronometer.sync(wire[lila.app.Env])
    lila.log("boot").info(s"Loaded lila modules in ${c.showDuration}")
    c.result

  val httpFilters = Seq(
    lila.web.HttpFilter(
      env.net,
      env.web.settings.sitewideCoepCredentiallessHeader.get,
      lila.security.Mobile.LichessMobileUa.parse
    )
  )

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

  lazy val devAssetsController =
    given FileMimeTypes = fileMimeTypes
    wire[ExternalAssets]
  lazy val account: Account               = wire[Account]
  lazy val analyse: Analyse               = wire[Analyse]
  lazy val api: Api                       = wire[Api]
  lazy val appealC: appeal.Appeal         = wire[appeal.Appeal]
  lazy val auth: Auth                     = wire[Auth]
  lazy val feed: Feed                     = wire[Feed]
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
  lazy val github: Github                 = wire[Github]
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
  lazy val plan: Plan                     = wire[Plan]
  lazy val practice: Practice             = wire[Practice]
  lazy val pref: Pref                     = wire[Pref]
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
  lazy val teamC: team.Team               = wire[team.Team]
  lazy val teamApi: TeamApi               = wire[TeamApi]
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
  lazy val recap: Recap                   = wire[Recap]
  lazy val bulkPairing: BulkPairing       = wire[BulkPairing]
  lazy val opening: Opening               = wire[Opening]
  lazy val cms: Cms                       = wire[Cms]
  lazy val fide: Fide                     = wire[Fide]
  lazy val titleVerify: TitleVerify       = wire[TitleVerify]

  // eagerly wire up all controllers
  private val appealRouter: _root_.router.appeal.Routes = wire[_root_.router.appeal.Routes]
  private val reportRouter: _root_.router.report.Routes = wire[_root_.router.report.Routes]
  private val clasRouter: _root_.router.clas.Routes     = wire[_root_.router.clas.Routes]
  private val teamRouter: _root_.router.team.Routes     = wire[_root_.router.team.Routes]
  val router: Router                                    = wire[_root_.router.router.Routes]

  lila.common.Uptime.startedAt
  UiEnv.setEnv(env)

  if configuration.get[Boolean]("kamon.enabled") then
    lila.log("boot").info("Kamon is enabled")
    kamon.Kamon.init()
