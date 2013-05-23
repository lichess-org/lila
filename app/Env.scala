package lila.app

import akka.actor._
import com.typesafe.config.Config

final class Env(config: Config, system: ActorSystem) {

  val CliUsername = config getString "cli.username"

  private val RendererName = config getString "app.renderer.name"
  private val RouterName = config getString "app.router.name"

  lazy val preloader = new mashup.Preload(
    lobby = Env.lobby.lobby,
    history = Env.lobby.history,
    featured = Env.game.featured)

  lazy val userInfo = mashup.UserInfo(
    countUsers = () ⇒ Env.user.countEnabled,
    bookmarkApi = Env.bookmark.api,
    eloCalculator = Env.round.eloCalculator,
    relationApi = Env.relation.api) _

  system.actorOf(Props(new actor.Renderer), name = RendererName)

  system.actorOf(Props(new actor.Router(
    baseUrl = Env.api.Net.BaseUrl,
    protocol = Env.api.Net.Protocol,
    domain = Env.api.Net.Domain
  )), name = RouterName)

  loginfo("[boot] Preloading modules")
  (Env.site,
    Env.tournament,
    Env.lobby,
    Env.game,
    Env.ai,
    Env.setup,
    Env.round,
    Env.team,
    Env.message,
    Env.socket,
    Env.timeline,
    Env.gameSearch,
    Env.teamSearch,
    Env.forumSearch,
    Env.relation)
  loginfo("[boot] Preloading complete")

  if (Env.ai.isServer) println("Running as AI server")
}

object Env {

  lazy val current = "[boot] app" describes new Env(
    config = lila.common.PlayApp.loadConfig,
    system = lila.common.PlayApp.system)

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
  def ai = lila.ai.Env.current
  def analyse = lila.analyse.Env.current
  def mod = lila.mod.Env.current
  def monitor = lila.monitor.Env.current
  def site = lila.site.Env.current
  def round = lila.round.Env.current
  def lobby = lila.lobby.Env.current
  def setup = lila.setup.Env.current
  def importer = lila.importer.Env.current
  def tournament = lila.tournament.Env.current
  def relation = lila.relation.Env.current
}
