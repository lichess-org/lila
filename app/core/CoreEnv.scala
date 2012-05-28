package lila
package core

import com.mongodb.casbah.MongoConnection
import com.mongodb.{ Mongo, MongoOptions, ServerAddress ⇒ MongoServer }

import akka.actor._

import play.api.libs.concurrent._
import play.api.Application

import ui._

final class CoreEnv private (application: Application, val settings: Settings) {

  implicit val app = application
  import settings._

  lazy val mongodb = new lila.mongodb.MongoDbEnv(
    settings = settings)

  lazy val i18n = new lila.i18n.I18nEnv(
    app = app,
    settings = settings)

  lazy val user = new lila.user.UserEnv(
    settings = settings,
    mongodb = mongodb.apply _,
    gameRepo = game.gameRepo) 

  lazy val forum = new lila.forum.ForumEnv(
    settings = settings,
    captcha = site.captcha,
    mongodb = mongodb.apply _,
    userRepo = user.userRepo)

  lazy val message = new lila.message.MessageEnv(
    settings = settings,
    mongodb = mongodb.apply _,
    userRepo = user.userRepo)

  lazy val wiki = new lila.wiki.WikiEnv(
    settings = settings,
    mongodb = mongodb.apply _)

  lazy val lobby = new lila.lobby.LobbyEnv(
    app = app,
    settings = settings,
    mongodb = mongodb.apply _,
    userRepo = user.userRepo,
    roundSocket = round.socket,
    roundMessenger = round.messenger)

  lazy val setup = new lila.setup.SetupEnv(
    settings = settings,
    mongodb = mongodb.apply _,
    gameRepo = game.gameRepo,
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
    userRepo = user.userRepo,
    eloUpdater = user.eloUpdater,
    i18nKeys = i18n.keys,
    ai = ai.ai)

  lazy val analyse = new lila.analyse.AnalyseEnv(
    settings = settings,
    gameRepo = game.gameRepo,
    userRepo = user.userRepo)

  lazy val site = new lila.site.SiteEnv(
    app = app,
    settings = settings,
    gameRepo = game.gameRepo)

  lazy val metaHub = new lila.socket.MetaHub(
    List(site.hub, lobby.hub, round.hubMaster))

  lazy val monitor = new lila.monitor.MonitorEnv(
    app = app,
    mongodb = mongodb.connection,
    settings = settings)

  lazy val preloader = new Preload(
    fisherman = lobby.fisherman,
    history = lobby.history,
    hookRepo = lobby.hookRepo,
    gameRepo = game.gameRepo,
    messageRepo = lobby.messageRepo,
    entryRepo = timeline.entryRepo)

  lazy val securityStore = new security.Store(
    collection = mongodb(MongoCollectionSecurity))

  lazy val gameFinishCommand = new command.GameFinish(
    gameRepo = game.gameRepo,
    finisher = round.finisher)

  lazy val gameCleanNextCommand = new command.GameCleanNext(gameRepo = game.gameRepo)
}

object CoreEnv {

  def apply(app: Application) = new CoreEnv(
    app,
    new Settings(app.configuration.underlying)
  )
}
