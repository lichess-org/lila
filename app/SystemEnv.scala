package lila

import com.mongodb.casbah.MongoConnection
import com.mongodb.{ Mongo, MongoOptions, ServerAddress ⇒ MongoServer }
import com.typesafe.config._
import akka.actor._

import play.api.libs.concurrent._
import play.api.Play.current

import chess.EloCalculator
import db._
import ai._
import memo._
import command._

final class SystemEnv(config: Config) {

  lazy val lobbyHub = Akka.system.actorOf(Props(new lobby.Hub(this)))

  lazy val lobbySocket = new lobby.Lobby(lobbyHub)

  lazy val appXhr = new AppXhr(
    gameRepo = gameRepo,
    messenger = messenger,
    ai = ai,
    finisher = finisher,
    versionMemo = versionMemo,
    aliveMemo = aliveMemo,
    moretimeSeconds = getSeconds("moretime.seconds"))

  lazy val appApi = new AppApi(
    gameRepo = gameRepo,
    versionMemo = versionMemo,
    aliveMemo = aliveMemo,
    messenger = messenger,
    starter = starter)

  lazy val appSyncer = new AppSyncer(
    gameRepo = gameRepo,
    versionMemo = versionMemo,
    aliveMemo = aliveMemo,
    duration = getMilliseconds("sync.duration"),
    sleep = getMilliseconds("sync.sleep"))

  lazy val lobbyApi = new lobby.Api(
    hookRepo = hookRepo,
    gameRepo = gameRepo,
    messenger = messenger,
    starter = starter,
    versionMemo = versionMemo,
    lobbyMemo = lobbyMemo,
    aliveMemo = aliveMemo,
    hookMemo = hookMemo)

  lazy val lobbyPreloader = new lobby.Preload(
    hookRepo = hookRepo,
    gameRepo = gameRepo,
    messageRepo = messageRepo,
    entryRepo = entryRepo,
    hookMemo = hookMemo)

  lazy val pinger = new Pinger(
    aliveMemo = aliveMemo,
    usernameMemo = usernameMemo,
    watcherMemo = watcherMemo,
    hookMemo = hookMemo)

  lazy val finisher = new Finisher(
    historyRepo = historyRepo,
    userRepo = userRepo,
    gameRepo = gameRepo,
    messenger = messenger,
    versionMemo = versionMemo,
    aliveMemo = aliveMemo,
    eloCalculator = new EloCalculator,
    finisherLock = new FinisherLock(
      timeout = getMilliseconds("memo.finisher_lock.timeout")))

  lazy val messenger = new Messenger(
    roomRepo = roomRepo)

  lazy val starter = new Starter(
    gameRepo = gameRepo,
    entryRepo = entryRepo,
    ai = ai,
    lobbySocket = lobbySocket,
    versionMemo = versionMemo)

  // tells whether the remote AI is healthy or not
  // frequently updated by a scheduled actor
  var remoteAiHealth = false

  def ai: () ⇒ Ai = () ⇒ config getString "ai.use" match {
    case "remote" ⇒ remoteAiHealth.fold(remoteAi, craftyAi)
    case "crafty" ⇒ craftyAi
    case _        ⇒ stupidAi
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

  lazy val versionMemo = new VersionMemo(
    getPov = gameRepo.pov,
    timeout = getMilliseconds("memo.version.timeout"))

  lazy val aliveMemo = new AliveMemo(
    hardTimeout = getMilliseconds("memo.alive.hard_timeout"),
    softTimeout = getMilliseconds("memo.alive.soft_timeout"))

  lazy val usernameMemo = new UsernameMemo(
    timeout = getMilliseconds("memo.username.timeout"))

  lazy val watcherMemo = new WatcherMemo(
    timeout = getMilliseconds("memo.watcher.timeout"))

  lazy val lobbyMemo = new LobbyMemo

  lazy val hookMemo = new HookMemo(
    timeout = getMilliseconds("memo.hook.timeout"))

  lazy val gameFinishCommand = new GameFinishCommand(
    gameRepo = gameRepo,
    finisher = finisher)

  def getMilliseconds(name: String): Int = (config getMilliseconds name).toInt

  def getSeconds(name: String): Int = getMilliseconds(name) / 1000
}

object SystemEnv extends EnvBuilder {

  def apply(overrides: String = "") = new SystemEnv(
    makeConfig(overrides)
  )
}

trait EnvBuilder {

  import java.io.File

  def makeConfig(sources: String*) = sources.foldLeft(ConfigFactory.load()) {
    case (config, source) if source isEmpty ⇒ config
    case (config, source) if source contains '=' ⇒
      config.withFallback(ConfigFactory parseString source)
    case (config, source) ⇒
      config.withFallback(ConfigFactory parseFile (new File(source)))
  }
}
