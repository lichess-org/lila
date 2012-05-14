package lila

import com.mongodb.casbah.MongoConnection
import com.mongodb.{ Mongo, MongoOptions, ServerAddress ⇒ MongoServer }

import com.typesafe.config._
import akka.actor._

import play.api.libs.concurrent._
import play.api.Application
import play.api.i18n.Lang
import play.api.i18n.MessagesPlugin

import db._
import ai._
import memo._
import i18n._
import ui._
import lobby.LobbyEnv
import user.UserEnv
import timeline.TimelineEnv

final class SystemEnv private(app: Application, settings: Settings) {

  implicit val ctx = app
  import settings._

  lazy val user = new UserEnv(
    settings = settings,
    mongodb = mongodb)

  lazy val lobby = new LobbyEnv(
    app = app,
    settings = settings,
    mongodb = mongodb,
    userRepo = user.userRepo,
    gameRepo = gameRepo)

  lazy val timeline = new TimelineEnv(
    settings = settings,
    mongodb = mongodb)

  lazy val setupFormFactory = new setup.FormFactory(userConfigRepo)

  lazy val i18nMessagesApi = app.plugin[MessagesPlugin]
    .err("this plugin was not registered or disabled")
    .api

  lazy val i18nPool = new I18nPool(
    langs = Lang.availables.toSet,
    default = Lang("en"))

  lazy val translator = new Translator(
    api = i18nMessagesApi,
    pool = i18nPool)

  lazy val i18nKeys = new I18nKeys(translator)
  
  lazy val i18nRequestHandler = new I18nRequestHandler(
    pool = i18nPool)

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
    starter = lobbyEnv.starter,
    eloUpdater = eloUpdater,
    gameInfo = gameInfo)

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
