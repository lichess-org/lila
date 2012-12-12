package lila
package core

import com.mongodb.casbah.MongoConnection
import com.mongodb.{ Mongo, MongoOptions, ServerAddress ⇒ MongoServer }

import akka.actor._

import play.api.libs.concurrent._
import play.api.Application
import play.api.Mode.Dev

import ui._

final class CoreEnv private (application: Application, val settings: Settings) {

  implicit val app = application
  import settings._

  def configName = ConfigName

  lazy val mongodb = new lila.mongodb.MongoDbEnv(
    settings = settings)

  lazy val i18n = new lila.i18n.I18nEnv(
    app = app,
    mongodb = mongodb.apply _,
    settings = settings,
    captcha = site.captcha)

  lazy val user = new lila.user.UserEnv(
    settings = settings,
    mongodb = mongodb.apply _,
    gameRepo = game.gameRepo)

  lazy val forum = new lila.forum.ForumEnv(
    settings = settings,
    captcha = site.captcha,
    mongodb = mongodb.apply _,
    userRepo = user.userRepo,
    modLog = mod.logApi)

  lazy val message = new lila.message.MessageEnv(
    settings = settings,
    mongodb = mongodb.apply _,
    userRepo = user.userRepo,
    notifyUnread = metaHub.notifyUnread)

  lazy val wiki = new lila.wiki.WikiEnv(
    settings = settings,
    mongodb = mongodb.apply _)

  lazy val team = new lila.team.TeamEnv(
    settings = settings,
    captcha = site.captcha,
    userRepo = user.userRepo,
    sendMessage = message.api.lichessThread,
    makeForum = forum.categApi.makeTeam,
    mongodb = mongodb.apply _)

  lazy val lobby = new lila.lobby.LobbyEnv(
    app = app,
    settings = settings,
    mongodb = mongodb.apply _,
    userRepo = user.userRepo,
    getGame = game.gameRepo.game,
    featured = game.featured,
    roundSocket = round.socket,
    roundMessenger = round.messenger,
    flood = security.flood)

  lazy val setup = new lila.setup.SetupEnv(
    settings = settings,
    mongodb = mongodb.apply _,
    gameRepo = game.gameRepo,
    pgnRepo = game.pgnRepo,
    hookRepo = lobby.hookRepo,
    fisherman = lobby.fisherman,
    userRepo = user.userRepo,
    timelinePush = timeline.push.apply,
    roundMessenger = round.messenger,
    ai = ai.ai)

  lazy val timeline = new lila.timeline.TimelineEnv(
    settings = settings,
    mongodb = mongodb.apply _,
    lobbyNotify = lobby.socket.addEntry,
    getUsername = user.cached.usernameOrAnonymous)

  lazy val ai = new lila.ai.AiEnv(
    settings = settings)

  lazy val game = new lila.game.GameEnv(
    settings = settings,
    mongodb = mongodb.apply _)

  lazy val round = new lila.round.RoundEnv(
    app = app,
    settings = settings,
    mongodb = mongodb.apply _,
    gameRepo = game.gameRepo,
    pgnRepo = game.pgnRepo,
    rewind = game.rewind,
    userRepo = user.userRepo,
    eloUpdater = user.eloUpdater,
    i18nKeys = i18n.keys,
    ai = ai.ai,
    countMove = monitor.mpsProvider.countRequest,
    flood = security.flood,
    indexGame = search.indexGame)

  lazy val tournament = new lila.tournament.TournamentEnv(
    app = app,
    settings = settings,
    getUser = user.userRepo.byId,
    gameRepo = game.gameRepo,
    timelinePush = timeline.push.apply,
    flood = security.flood,
    siteSocket = site.socket,
    lobbySocket = lobby.socket,
    roundMeddler = round.meddler,
    incToints = user.userRepo.incToints,
    mongodb = mongodb.apply _)

  lazy val analyse = new lila.analyse.AnalyseEnv(
    settings = settings,
    gameRepo = game.gameRepo,
    pgnRepo = game.pgnRepo,
    userRepo = user.userRepo,
    mongodb = mongodb.apply _,
    () ⇒ ai.ai().analyse _)

  lazy val bookmark = new lila.bookmark.BookmarkEnv(
    settings = settings,
    gameRepo = game.gameRepo,
    userRepo = user.userRepo,
    mongodb = mongodb.apply _)

  lazy val site = new lila.site.SiteEnv(
    app = app,
    settings = settings,
    gameRepo = game.gameRepo,
    pgnRepo = game.pgnRepo)

  lazy val security = new lila.security.SecurityEnv(
    settings = settings,
    captcha = site.captcha,
    mongodb = mongodb.apply _,
    userRepo = user.userRepo)

  lazy val search = new lila.search.SearchEnv(
    settings = settings,
    mongodb = mongodb.apply _,
    gameRepo = game.gameRepo,
    pgnRepo = game.pgnRepo)

  lazy val metaHub = new lila.socket.MetaHub(
    List(site.hub, lobby.hub, round.hubMaster, tournament.hubMaster))

  lazy val notificationApi = new lila.notification.Api(
    metaHub = metaHub)

  lazy val monitor = new lila.monitor.MonitorEnv(
    app = app,
    mongodb = mongodb.connection,
    settings = settings)

  lazy val titivate = new lila.core.Titivate(
    gameRepo = game.gameRepo,
    pgnRepo = game.pgnRepo,
    finisher = round.finisher,
    meddler = round.meddler,
    bookmarkApi = bookmark.api)

  lazy val mod = new lila.mod.ModEnv(
    settings = settings,
    userRepo = user.userRepo,
    securityStore = security.store,
    firewall = security.firewall,
    eloUpdater = user.eloUpdater,
    lobbyMessenger = lobby.messenger,
    mongodb = mongodb.apply _)
}

object CoreEnv {

  def apply(app: Application) = new CoreEnv(
    app,
    new Settings(app.configuration.underlying, app.mode == Dev)
  )
}
