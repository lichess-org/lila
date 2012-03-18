package lila.system

import com.mongodb.casbah.MongoConnection
import com.typesafe.config._

import db._
import ai._
import memo._

trait SystemEnv {

  protected val config: Config

  def server = new Server(
    repo = gameRepo,
    ai = ai,
    versionMemo = versionMemo,
    aliveMemo = aliveMemo)

  def internalApi = new InternalApi(
    repo = gameRepo,
    versionMemo = versionMemo,
    aliveMemo = aliveMemo)

  def syncer = new Syncer(
    repo = gameRepo,
    versionMemo = versionMemo,
    aliveMemo = aliveMemo,
    duration = (config getMilliseconds "sync.duration").toInt,
    sleep = (config getMilliseconds "sync.sleep").toInt)

  def pinger = new Pinger(
    aliveMemo = aliveMemo,
    usernameMemo = usernameMemo,
    watcherMemo = watcherMemo)

  lazy val ai: Ai = new CraftyAi(
    execPath = config getString "crafty.exec_path",
    bookPath = Some(config getString "crafty.book_path") filter ("" !=))

  def gameRepo = new GameRepo(
    mongodb(config getString "mongo.collection.game"))

  def mongodb = MongoConnection(
    config getString "mongo.host",
    config getInt "mongo.port"
  )(config getString "mongo.dbName")

  lazy val versionMemo = new VersionMemo(
    getPlayer = gameRepo.playerOnly,
    timeout = (config getMilliseconds "memo.version.timeout").toInt)

  lazy val aliveMemo = new AliveMemo(
    hardTimeout = (config getMilliseconds "memo.alive.hard_timeout").toInt,
    softTimeout = (config getMilliseconds "memo.alive.soft_timeout").toInt)

  lazy val usernameMemo = new UsernameMemo(
    timeout = (config getMilliseconds "memo.username.timeout").toInt)

  lazy val watcherMemo = new WatcherMemo(
    timeout = (config getMilliseconds "memo.watcher.timeout").toInt)
}

object SystemEnv extends EnvBuilder {

  def apply(overrides: String = "") = new SystemEnv {
    val config = makeConfig(overrides, "/home/thib/lila/conf/application.conf")
  }
}

trait EnvBuilder {

  import java.io.File

  def makeConfig(sources: String*) = sources.foldLeft(ConfigFactory.defaultOverrides) {
    case (config, source) if source isEmpty ⇒ config
    case (config, source) if source contains '=' ⇒
      config.withFallback(ConfigFactory parseString source)
    case (config, source) ⇒
      config.withFallback(ConfigFactory parseFile (new File(source)))
  }
}
