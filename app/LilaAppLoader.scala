import com.softwaremill.macwire._
import play.api._
import play.api.mvc._
import play.api.routing.Router
import router.Routes

final class LilaAppLoader extends ApplicationLoader {
  def load(ctx: ApplicationLoader.Context): Application = new LilaComponents(ctx).application
}

final class LilaComponents(ctx: ApplicationLoader.Context) extends BuiltInComponentsFromContext(ctx)
  with _root_.controllers.AssetsComponents
  with play.api.libs.ws.ahc.AhcWSComponents {

  LoggerConfigurator(ctx.environment.classLoader).foreach {
    _.configure(ctx.environment, ctx.initialConfiguration, Map.empty)
  }

  import _root_.controllers._

  lazy val httpFilters = Seq(wire[lila.app.LilaHttpFilter])

  implicit def system = actorSystem
  implicit def ws = wsClient
  def cookieBacker: SessionCookieBaker = new DefaultSessionCookieBaker(
    httpConfiguration.session,
    httpConfiguration.secret,
    new libs.crypto.CookieSignerProvider(httpConfiguration.secret).get
  )

  lazy val boot: lila.app.EnvBoot = wire[lila.app.EnvBoot]
  lazy val env: lila.app.Env = boot.env

  lazy val account: Account = wire[Account]
  lazy val analyse: Analyse = wire[Analyse]
  lazy val api: Api = wire[Api]
  lazy val auth: Auth = wire[Auth]
  lazy val blog: Blog = wire[Blog]
  lazy val bookmark: Bookmark = wire[Bookmark]
  lazy val bot: Bot = wire[Bot]
  lazy val challenge: Challenge = wire[Challenge]
  lazy val coach: Coach = wire[Coach]
  lazy val coordinate: Coordinate = wire[Coordinate]
  lazy val dasher: Dasher = wire[Dasher]
  lazy val dev: Dev = wire[Dev]
  lazy val editor: Editor = wire[Editor]
  lazy val event: Event = wire[Event]
  lazy val export: Export = wire[Export]
  lazy val fishnet: Fishnet = wire[Fishnet]
  lazy val forumCateg: ForumCateg = wire[ForumCateg]
  lazy val forumPost: ForumPost = wire[ForumPost]
  lazy val forumTopic: ForumTopic = wire[ForumTopic]
  lazy val game: Game = wire[Game]
  lazy val i18n: I18n = wire[I18n]
  lazy val importer: Importer = wire[Importer]
  lazy val insight: Insight = wire[Insight]
  lazy val irwin: Irwin = wire[Irwin]
  lazy val learn: Learn = wire[Learn]
  lazy val lobby: Lobby = wire[Lobby]
  lazy val main: Main = wire[Main]
  lazy val message: Message = wire[Message]
  lazy val mod: Mod = wire[Mod]
  lazy val notifyC: Notify = wire[Notify]
  lazy val oAuthApp: OAuthApp = wire[OAuthApp]
  lazy val oAuthToken: OAuthToken = wire[OAuthToken]
  lazy val options: Options = wire[Options]
  lazy val page: Page = wire[Page]
  lazy val plan: Plan = wire[Plan]
  lazy val practice: Practice = wire[Practice]
  lazy val pref: Pref = wire[Pref]
  lazy val prismic: Prismic = wire[Prismic]
  lazy val push: Push = wire[Push]
  lazy val puzzle: Puzzle = wire[Puzzle]
  lazy val relation: Relation = wire[Relation]
  lazy val relay: Relay = wire[Relay]
  lazy val report: Report = wire[Report]
  lazy val round: Round = wire[Round]
  lazy val search: Search = wire[Search]
  lazy val setup: Setup = wire[Setup]
  lazy val simul: Simul = wire[Simul]
  lazy val stat: Stat = wire[Stat]
  lazy val streamer: Streamer = wire[Streamer]
  lazy val study: Study = wire[Study]
  lazy val team: Team = wire[Team]
  lazy val timeline: Timeline = wire[Timeline]
  lazy val tournament: Tournament = wire[Tournament]
  lazy val tournamentCrud: TournamentCrud = wire[TournamentCrud]
  lazy val tv: Tv = wire[Tv]
  lazy val user: User = wire[User]
  lazy val userAnalysis: UserAnalysis = wire[UserAnalysis]
  lazy val userTournament: UserTournament = wire[UserTournament]
  lazy val video: Video = wire[Video]

  // eagerly wire up all controllers
  val router: Router = {
    val prefix: String = "/"
    wire[Routes]
  }

  if (configuration.get[String]("kamon.influxdb.hostname").nonEmpty) {
    lila.log("boot").info("Kamon is enabled")
    kamon.Kamon.loadModules()
  }
}
