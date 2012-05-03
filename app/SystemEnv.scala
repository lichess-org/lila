package lila

import com.mongodb.casbah.MongoConnection
import com.mongodb.{ Mongo, MongoOptions, ServerAddress ⇒ MongoServer }
import com.typesafe.config._
import akka.actor._

import play.api.libs.concurrent._
import play.api.Application

import chess.EloCalculator
import db._
import ai._
import memo._

final class SystemEnv(application: Application) {

  implicit val app = application
  val config = app.configuration.underlying

  lazy val reporting = Akka.system.actorOf(
    Props(new report.Reporting), name = "reporting")

  lazy val siteHub = Akka.system.actorOf(
    Props(new site.Hub(
      timeout = getMilliseconds("site.uid.timeout")
  )), name = "site_hub")

  lazy val siteSocket = new site.Socket(
    hub = siteHub)

  lazy val gameHistory = () ⇒ new game.History(
    timeout = getMilliseconds("game.message.lifetime"))

  lazy val gameHubMaster = Akka.system.actorOf(Props(new game.HubMaster(
    makeHistory = gameHistory,
    uidTimeout = getMilliseconds("game.uid.timeout"),
    hubTimeout = getMilliseconds("game.hub.timeout")
  )), name = "game_hub_master")

  lazy val gameSocket = new game.Socket(
    getGame = gameRepo.game,
    hand = hand,
    hubMaster = gameHubMaster,
    messenger = messenger)

  lazy val lobbyHistory = new lobby.History(
    timeout = getMilliseconds("lobby.message.lifetime"))

  lazy val lobbyHub = Akka.system.actorOf(Props(new lobby.Hub(
    messageRepo = messageRepo,
    history = lobbyHistory,
    timeout = getMilliseconds("site.uid.timeout")
  )), name = "lobby_hub")

  lazy val lobbySocket = new lobby.Socket(
    hub = lobbyHub)

  lazy val lobbyPreloader = new lobby.Preload(
    fisherman = lobbyFisherman,
    history = lobbyHistory,
    hookRepo = hookRepo,
    gameRepo = gameRepo,
    messageRepo = messageRepo,
    entryRepo = entryRepo)

  lazy val lobbyFisherman = new lobby.Fisherman(
    hookRepo = hookRepo,
    hookMemo = hookMemo,
    socket = lobbySocket)

  lazy val hand = new Hand(
    gameRepo = gameRepo,
    messenger = messenger,
    ai = ai,
    finisher = finisher,
    moretimeSeconds = getSeconds("moretime.seconds"))

  lazy val appApi = new AppApi(
    userRepo = userRepo,
    gameRepo = gameRepo,
    gameSocket = gameSocket,
    messenger = messenger,
    starter = starter,
    eloUpdater = eloUpdater)

  lazy val lobbyApi = new lobby.Api(
    hookRepo = hookRepo,
    fisherman = lobbyFisherman,
    gameRepo = gameRepo,
    gameSocket = gameSocket,
    messenger = messenger,
    starter = starter,
    lobbySocket = lobbySocket)

  lazy val captcha = new Captcha(gameRepo = gameRepo)

  lazy val finisher = new Finisher(
    userRepo = userRepo,
    gameRepo = gameRepo,
    messenger = messenger,
    eloUpdater = eloUpdater,
    eloCalculator = new EloCalculator,
    finisherLock = new FinisherLock(
      timeout = getMilliseconds("memo.finisher_lock.timeout")))

  lazy val messenger = new Messenger(
    roomRepo = roomRepo)

  lazy val starter = new Starter(
    gameRepo = gameRepo,
    entryRepo = entryRepo,
    ai = ai,
    lobbySocket = lobbySocket)

  lazy val eloUpdater = new EloUpdater(
    userRepo = userRepo,
    historyRepo = historyRepo)

  val ai: () ⇒ Ai = config getString "ai.use" match {
    case "remote" ⇒ () ⇒ remoteAi or craftyAi
    case "crafty" ⇒ () ⇒ craftyAi
    case _        ⇒ () ⇒ stupidAi
  }

  lazy val remoteAi = new RemoteAi(
    remoteUrl = config getString "ai.remote.url")

  lazy val craftyAi = new CraftyAi(
    server = craftyServer)

  lazy val craftyServer = new CraftyServer(
    execPath = config getString "ai.crafty.exec_path",
    bookPath = Some(config getString "ai.crafty.book_path") filter ("" !=))

  lazy val stupidAi = new StupidAi

  def isAiServer = config getBoolean "ai.server"

  lazy val gameRepo = new GameRepo(
    mongodb(config getString "mongo.collection.game"))

  lazy val userRepo = new UserRepo(
    mongodb(config getString "mongo.collection.user"))

  lazy val hookRepo = new HookRepo(
    mongodb(config getString "mongo.collection.hook"))

  lazy val entryRepo = new EntryRepo(
    collection = mongodb(config getString "mongo.collection.entry"),
    max = config getInt "lobby.entry.max")

  lazy val messageRepo = new MessageRepo(
    collection = mongodb(config getString "mongo.collection.message"),
    max = config getInt "lobby.message.max")

  lazy val historyRepo = new HistoryRepo(
    collection = mongodb(config getString "mongo.collection.history"))

  lazy val roomRepo = new RoomRepo(
    collection = mongodb(config getString "mongo.collection.room"))

  lazy val mongodb = MongoConnection(
    new MongoServer(config getString "mongo.host", config getInt "mongo.port"),
    mongoOptions
  )(config getString "mongo.dbName")

  // http://stackoverflow.com/questions/6520439/how-to-configure-mongodb-java-driver-mongooptions-for-production-use
  private val mongoOptions = new MongoOptions() ~ { o ⇒
    o.connectionsPerHost = config getInt "mongo.connectionsPerHost"
    o.autoConnectRetry = config getBoolean "mongo.autoConnectRetry"
    o.connectTimeout = getMilliseconds("mongo.connectTimeout")
    o.threadsAllowedToBlockForConnectionMultiplier = config getInt "mongo.threadsAllowedToBlockForConnectionMultiplier"
  }

  lazy val hookMemo = new HookMemo(
    timeout = getMilliseconds("memo.hook.timeout"))

  lazy val gameFinishCommand = new command.GameFinish(
    gameRepo = gameRepo,
    finisher = finisher)

  lazy val gameCleanNextCommand = new command.GameCleanNext(
    gameRepo = gameRepo)

  def getMilliseconds(name: String): Int = (config getMilliseconds name).toInt

  def getSeconds(name: String): Int = getMilliseconds(name) / 1000
}
