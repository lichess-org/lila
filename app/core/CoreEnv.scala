package lila.app
package core

import com.mongodb.casbah.MongoConnection
import com.mongodb.{ Mongo, MongoOptions, ServerAddress ⇒ MongoServer }

import akka.actor._

import play.api.libs.concurrent._
import play.api.Application
import play.api.Mode.Dev

import play.modules.reactivemongo._

import ui._

final class CoreEnv private (application: Application, val settings: Settings) {

  implicit val app = application
  import settings._

  def configName = ConfigName

  def mongodb = new lila.app.mongodb.MongoDbEnv(
    settings = settings)

  lazy val mongo2 = new lila.app.mongodb.Mongo2Env(
    db = ReactiveMongoPlugin.db, 
    settings = settings)

  lazy val i18n = new lila.app.i18n.I18nEnv(
    app = app,
    mongodb = mongodb.apply _,
    settings = settings,
    captcha = site.captcha)

  lazy val user = new lila.app.user.UserEnv(
    settings = settings,
    mongodb = mongodb.apply _,
    gameRepo = game.gameRepo)

  lazy val forum = new lila.app.forum.ForumEnv(
    settings = settings,
    esIndexer = search.esIndexer,
    captcha = site.captcha,
    mongodb = mongodb.apply _,
    userRepo = user.userRepo,
    modLog = mod.logApi)

  lazy val message = new lila.app.message.MessageEnv(
    settings = settings,
    mongodb = mongodb.apply _,
    userRepo = user.userRepo,
    notifyUnread = metaHub.notifyUnread)

  lazy val wiki = new lila.app.wiki.WikiEnv(
    settings = settings,
    mongodb = mongodb.apply _)

  lazy val team = new lila.app.team.TeamEnv(
    settings = settings,
    esIndexer = search.esIndexer,
    captcha = site.captcha,
    userRepo = user.userRepo,
    sendMessage = message.api.lichessThread,
    makeForum = forum.categApi.makeTeam,
    getForumNbPosts = forum.categApi.getTeamNbPosts,
    getForumPosts = forum.recent.team,
    mongodb = mongodb.apply _)

  lazy val lobby = new lila.app.lobby.LobbyEnv(
    app = app,
    settings = settings,
    mongodb = mongodb.apply _,
    userRepo = user.userRepo,
    getGame = game.gameRepo.game,
    featured = game.featured,
    roundSocket = round.socket,
    roundMessenger = round.messenger,
    flood = security.flood)

  lazy val setup = new lila.app.setup.SetupEnv(
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

  lazy val timeline = new lila.app.timeline.TimelineEnv(
    settings = settings,
    mongodb = mongodb.apply _,
    lobbyNotify = lobby.socket.addEntry,
    getUsername = user.cached.usernameOrAnonymous)

  lazy val ai = new lila.app.ai.AiEnv(
    settings = settings)

  lazy val game = new lila.app.game.GameEnv(
    app = app,
    settings = settings,
    mongodb = mongodb.apply _)

  lazy val round = new lila.app.round.RoundEnv(
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

  lazy val importer = new lila.app.importer.ImporterEnv(
    gameRepo = game.gameRepo,
    hand = round.hand,
    finisher = round.finisher,
    bookmark = bookmark.api.toggle _)

  lazy val tournament = new lila.app.tournament.TournamentEnv(
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

  lazy val analyse = new lila.app.analyse.AnalyseEnv(
    settings = settings,
    gameRepo = game.gameRepo,
    pgnRepo = game.pgnRepo,
    userRepo = user.userRepo,
    mongodb = mongodb.apply _,
    () ⇒ ai.ai().analyse _)

  lazy val bookmark = new lila.app.bookmark.BookmarkEnv(
    settings = settings,
    gameRepo = game.gameRepo,
    userRepo = user.userRepo,
    mongodb = mongodb.apply _)

  lazy val site = new lila.app.site.SiteEnv(
    app = app,
    settings = settings,
    gameRepo = game.gameRepo,
    pgnRepo = game.pgnRepo)

  lazy val security = new lila.app.security.SecurityEnv(
    settings = settings,
    captcha = site.captcha,
    mongodb = mongodb.apply _,
    userRepo = user.userRepo)

  lazy val search = new lila.app.search.SearchEnv(
    settings = settings,
    mongodb = mongodb.apply _,
    gameRepo = game.gameRepo,
    pgnRepo = game.pgnRepo)

  lazy val metaHub = new lila.app.socket.MetaHub(
    List(site.hub, lobby.hub, round.hubMaster, tournament.hubMaster))

  lazy val notificationApi = new lila.app.notification.Api(
    metaHub = metaHub)

  lazy val monitor = new lila.app.monitor.MonitorEnv(
    app = app,
    mongodb = mongodb.connection,
    settings = settings)

  lazy val titivate = new lila.app.core.Titivate(
    gameRepo = game.gameRepo,
    pgnRepo = game.pgnRepo,
    finisher = round.finisher,
    meddler = round.meddler,
    bookmarkApi = bookmark.api)

  lazy val mod = new lila.app.mod.ModEnv(
    settings = settings,
    userRepo = user.userRepo,
    userSpy = security.store.userSpy _,
    firewall = security.firewall,
    eloUpdater = user.eloUpdater,
    lobbyMessenger = lobby.messenger,
    mongodb = mongodb.apply _,
    db = mongo2.db)
}

object CoreEnv {

  def apply(app: Application) = new CoreEnv(
    app,
    new Settings(app.configuration.underlying, app.mode == Dev)
  )
}
