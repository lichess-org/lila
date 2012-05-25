package lila
package core

import com.mongodb.casbah.MongoConnection
import com.mongodb.{ DBRef, Mongo, MongoOptions, ServerAddress ⇒ MongoServer }

import akka.actor._

import play.api.libs.concurrent._
import play.api.Application

import ui._

final class CoreEnv private (application: Application, val settings: Settings) {

  implicit val app = application
  import settings._

  lazy val i18n = new lila.i18n.I18nEnv(
    app = app,
    settings = settings)

  lazy val user = new lila.user.UserEnv(
    settings = settings,
    mongodb = mongodb.apply _,
    gameRepo = game.gameRepo,
    dbRef = namespace ⇒ id ⇒ new DBRef(mongodb.underlying, namespace, id))

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
    fisherman = lobby.fisherman,
    userRepo = user.userRepo,
    timelinePush = timeline.push.apply,
    roundMessenger = round.messenger,
    ai = ai.ai,
    dbRef = user.userRepo.dbRef)

  lazy val timeline = new lila.timeline.TimelineEnv(
    settings = settings,
    mongodb = mongodb.apply _,
    lobbyNotify = lobby.socket.addEntry,
    getUsername = user.cached.username)

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

  lazy val monitor = new lila.monitor.MonitorEnv(
    app = app,
    mongodb = mongodb,
    settings = settings)

  lazy val preloader = new Preload(
    fisherman = lobby.fisherman,
    history = lobby.history,
    hookRepo = lobby.hookRepo,
    gameRepo = game.gameRepo,
    messageRepo = lobby.messageRepo,
    entryRepo = timeline.entryRepo)

  lazy val mongoCache = new MongoCache(mongodb(MongoCollectionCache))

  lazy val mongodb = MongoConnection(
    new MongoServer(MongoHost, MongoPort),
    mongoOptions
  )(MongoDbName)

  // http://stackoverflow.com/questions/6520439/how-to-configure-mongodb-java-driver-mongooptions-for-production-use
  private val mongoOptions = new MongoOptions() ~ { o ⇒
    o.connectionsPerHost = MongoConnectionsPerHost
    o.autoConnectRetry = MongoAutoConnectRetry
    o.connectTimeout = MongoConnectTimeout
    o.threadsAllowedToBlockForConnectionMultiplier = MongoBlockingThreads
  }

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
