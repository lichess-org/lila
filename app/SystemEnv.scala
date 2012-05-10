package lila

import com.mongodb.casbah.MongoConnection
import com.mongodb.{ Mongo, MongoOptions, ServerAddress ⇒ MongoServer }
import com.typesafe.config._
import akka.actor._

import play.api.libs.concurrent._
import play.api.Application

import db._
import ai._
import memo._

final class SystemEnv private (application: Application, settings: Settings) {

  implicit val app = application

  import settings._

  lazy val pgnDump = new PgnDump(userRepo = userRepo, gameRepo = gameRepo)

  lazy val gameInfo = GameInfo(pgnDump) _

  lazy val reporting = Akka.system.actorOf(
    Props(new report.Reporting), name = ActorReporting)

  lazy val siteHub = Akka.system.actorOf(
    Props(new site.Hub(timeout = SiteUidTimeout)), name = ActorSiteHub)

  lazy val siteSocket = new site.Socket(hub = siteHub)

  lazy val gameHistory = () ⇒ new game.History(timeout = GameMessageLifetime)

  lazy val gameHubMaster = Akka.system.actorOf(Props(new game.HubMaster(
    makeHistory = gameHistory,
    uidTimeout = GameUidTimeout,
    hubTimeout = GameHubTimeout,
    playerTimeout = GamePlayerTimeout
  )), name = ActorGameHubMaster)

  lazy val gameSocket = new game.Socket(
    getGame = gameRepo.game,
    hand = hand,
    hubMaster = gameHubMaster,
    messenger = messenger)

  lazy val lobbyHistory = new lobby.History(timeout = LobbyMessageLifetime)

  lazy val lobbyMessenger = new lobby.Messenger(
    messageRepo = messageRepo,
    userRepo = userRepo)

  lazy val lobbyHub = Akka.system.actorOf(Props(new lobby.Hub(
    messenger = lobbyMessenger,
    history = lobbyHistory,
    timeout = SiteUidTimeout
  )), name = ActorLobbyHub)

  lazy val lobbySocket = new lobby.Socket(hub = lobbyHub)

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
    takeback = takeback,
    hubMaster = gameHubMaster,
    moretimeSeconds = MoretimeSeconds)

  lazy val appApi = new AppApi(
    userRepo = userRepo,
    gameRepo = gameRepo,
    gameSocket = gameSocket,
    messenger = messenger,
    starter = starter,
    eloUpdater = eloUpdater,
    gameInfo = gameInfo)

  lazy val lobbyApi = new lobby.Api(
    hookRepo = hookRepo,
    fisherman = lobbyFisherman,
    gameRepo = gameRepo,
    gameSocket = gameSocket,
    gameMessenger = messenger,
    starter = starter,
    lobbySocket = lobbySocket)

  lazy val captcha = new Captcha(gameRepo = gameRepo)

  lazy val finisher = new Finisher(
    userRepo = userRepo,
    gameRepo = gameRepo,
    messenger = messenger,
    eloUpdater = eloUpdater,
    eloCalculator = eloCalculator,
    finisherLock = finisherLock)

  lazy val eloCalculator = new chess.EloCalculator

  lazy val finisherLock = new FinisherLock(timeout = FinisherLockTimeout)

  lazy val takeback = new Takeback(gameRepo = gameRepo, messenger = messenger)

  lazy val messenger = new Messenger(roomRepo = roomRepo)

  lazy val starter = new Starter(
    gameRepo = gameRepo,
    entryRepo = entryRepo,
    ai = ai,
    lobbySocket = lobbySocket)

  lazy val eloUpdater = new EloUpdater(
    userRepo = userRepo,
    historyRepo = historyRepo)

  val ai: () ⇒ Ai = AiChoice match {
    case AiRemote ⇒ () ⇒ remoteAi or craftyAi
    case AiCrafty ⇒ () ⇒ craftyAi
    case _        ⇒ () ⇒ stupidAi
  }

  lazy val remoteAi = new RemoteAi(remoteUrl = AiRemoteUrl)

  lazy val craftyAi = new CraftyAi(server = craftyServer)

  lazy val craftyServer = new CraftyServer(
    execPath = AiCraftyExecPath,
    bookPath = AiCraftyBookPath)

  lazy val stupidAi = new StupidAi

  def isAiServer = AiServerMode

  lazy val gameRepo = new GameRepo(mongodb(MongoCollectionGame))

  lazy val userRepo = new UserRepo(mongodb(MongoCollectionUser))

  lazy val hookRepo = new HookRepo(mongodb(MongoCollectionHook))

  lazy val entryRepo = new EntryRepo(
    collection = mongodb(MongoCollectionEntry),
    max = LobbyEntryMax)

  lazy val messageRepo = new MessageRepo(
    collection = mongodb(MongoCollectionMessage),
    max = LobbyMessageMax)

  lazy val historyRepo = new HistoryRepo(mongodb(MongoCollectionHistory))

  lazy val roomRepo = new RoomRepo(mongodb(MongoCollectionRoom))

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

  lazy val hookMemo = new HookMemo(timeout = MemoHookTimeout)

  lazy val usernameMemo = new UsernameMemo(timeout = MemoUsernameTimeout)

  lazy val gameFinishCommand = new command.GameFinish(
    gameRepo = gameRepo,
    finisher = finisher)

  lazy val gameCleanNextCommand = new command.GameCleanNext(gameRepo = gameRepo)
}

object SystemEnv {

  def apply(app: Application) = new SystemEnv(
    app, 
    new Settings(app.configuration.underlying)
  )
}
