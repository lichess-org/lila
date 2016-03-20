package lila.app

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

final class Env(
    config: Config,
    val scheduler: lila.common.Scheduler,
    system: ActorSystem,
    appPath: String) {

  val CliUsername = config getString "cli.username"

  private val RendererName = config getString "app.renderer.name"
  private val RouterName = config getString "app.router.name"
  private val WebPath = config getString "app.web_path"

  lazy val bus = lila.common.Bus(system)

  lazy val preloader = new mashup.Preload(
    tv = Env.tv.tv,
    leaderboard = Env.user.cached.topWeek,
    tourneyWinners = Env.tournament.winners.scheduled,
    timelineEntries = Env.timeline.entryRepo.userEntries _,
    dailyPuzzle = Env.puzzle.daily,
    streamsOnAir = () => Env.tv.streamsOnAir.all,
    countRounds = Env.round.count,
    lobbyApi = Env.api.lobbyApi,
    getPlayban = Env.playban.api.currentBan _,
    lightUser = Env.user.lightUser)

  lazy val userInfo = mashup.UserInfo(
    countUsers = () => Env.user.countEnabled,
    bookmarkApi = Env.bookmark.api,
    relationApi = Env.relation.api,
    trophyApi = Env.user.trophyApi,
    gameCached = Env.game.cached,
    crosstableApi = Env.game.crosstableApi,
    postApi = Env.forum.postApi,
    getRatingChart = Env.history.ratingChartApi.apply,
    getRanks = Env.user.cached.ranking.getAll,
    isDonor = Env.donation.isDonor,
    isHostingSimul = Env.simul.isHosting,
    isStreamer = Env.tv.isStreamer.apply,
    insightShare = Env.insight.share) _

  system.actorOf(Props(new actor.Renderer), name = RendererName)

  system.actorOf(Props(new actor.Router(
    baseUrl = Env.api.Net.BaseUrl,
    protocol = Env.api.Net.Protocol,
    domain = Env.api.Net.Domain
  )), name = RouterName)

  lila.log.boot.info("Preloading modules")
  List(Env.socket,
    Env.site,
    Env.tournament,
    Env.lobby,
    Env.game,
    Env.setup,
    Env.round,
    Env.team,
    Env.message,
    Env.timeline,
    Env.gameSearch,
    Env.teamSearch,
    Env.forumSearch,
    Env.relation,
    Env.report,
    Env.notification,
    Env.bookmark,
    Env.pref,
    Env.chat,
    Env.puzzle,
    Env.tv,
    Env.blog,
    Env.video,
    Env.shutup, // required to load the actor
    Env.insight, // required to load the actor
    Env.worldMap, // required to load the actor
    Env.push, // required to load the actor
    Env.perfStat, // required to load the actor
    Env.slack, // required to load the actor
    Env.challenge, // required to load the actor
    Env.explorer, // required to load the actor
    Env.fishnet // required to schedule the cleaner
  )
  play.api.Logger("boot").info("Preloading complete")

  scheduler.once(5 seconds) {
    Env.slack.api.publishInfo("Lichess has restarted!")
  }
}

object Env {

  lazy val current = "app" boot new Env(
    config = lila.common.PlayApp.loadConfig,
    scheduler = lila.common.PlayApp.scheduler,
    system = lila.common.PlayApp.system,
    appPath = lila.common.PlayApp withApp (_.path.getCanonicalPath))

  def api = lila.api.Env.current
  def db = lila.db.Env.current
  def user = lila.user.Env.current
  def security = lila.security.Env.current
  def wiki = lila.wiki.Env.current
  def hub = lila.hub.Env.current
  def socket = lila.socket.Env.current
  def message = lila.message.Env.current
  def notification = lila.notification.Env.current
  def i18n = lila.i18n.Env.current
  def game = lila.game.Env.current
  def bookmark = lila.bookmark.Env.current
  def search = lila.search.Env.current
  def gameSearch = lila.gameSearch.Env.current
  def timeline = lila.timeline.Env.current
  def forum = lila.forum.Env.current
  def forumSearch = lila.forumSearch.Env.current
  def team = lila.team.Env.current
  def teamSearch = lila.teamSearch.Env.current
  def analyse = lila.analyse.Env.current
  def mod = lila.mod.Env.current
  def site = lila.site.Env.current
  def round = lila.round.Env.current
  def lobby = lila.lobby.Env.current
  def setup = lila.setup.Env.current
  def importer = lila.importer.Env.current
  def tournament = lila.tournament.Env.current
  def simul = lila.simul.Env.current
  def relation = lila.relation.Env.current
  def report = lila.report.Env.current
  def pref = lila.pref.Env.current
  def chat = lila.chat.Env.current
  def puzzle = lila.puzzle.Env.current
  def coordinate = lila.coordinate.Env.current
  def tv = lila.tv.Env.current
  def blog = lila.blog.Env.current
  def donation = lila.donation.Env.current
  def qa = lila.qa.Env.current
  def history = lila.history.Env.current
  def worldMap = lila.worldMap.Env.current
  def opening = lila.opening.Env.current
  def video = lila.video.Env.current
  def playban = lila.playban.Env.current
  def shutup = lila.shutup.Env.current
  def insight = lila.insight.Env.current
  def push = lila.push.Env.current
  def perfStat = lila.perfStat.Env.current
  def slack = lila.slack.Env.current
  def challenge = lila.challenge.Env.current
  def explorer = lila.explorer.Env.current
  def fishnet = lila.fishnet.Env.current
}
