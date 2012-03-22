package lila.system

import com.mongodb.casbah.MongoConnection
import com.typesafe.config._

import db._
import ai._
import memo._

final class SystemEnv(config: Config) {

  lazy val appXhr = new AppXhr(
    gameRepo = gameRepo,
    ai = ai,
    versionMemo = versionMemo,
    aliveMemo = aliveMemo)

  lazy val appApi = new AppApi(
    gameRepo = gameRepo,
    versionMemo = versionMemo,
    aliveMemo = aliveMemo)

  lazy val lobbyXhr = new LobbyXhr(
    hookRepo = hookRepo,
    lobbyMemo = lobbyMemo,
    duration = getMilliseconds("lobby.poll.duration"),
    sleep = getMilliseconds("lobby.poll.sleep"))

  lazy val lobbyApi = new LobbyApi(
    hookRepo = hookRepo,
    versionMemo = versionMemo,
    lobbyMemo = lobbyMemo,
    gameRepo = gameRepo,
    aliveMemo = aliveMemo,
    hookMemo = hookMemo)

  lazy val syncer = new Syncer(
    gameRepo = gameRepo,
    versionMemo = versionMemo,
    aliveMemo = aliveMemo,
    duration = getMilliseconds("sync.duration"),
    sleep = getMilliseconds("sync.sleep"))

  lazy val pinger = new Pinger(
    aliveMemo = aliveMemo,
    usernameMemo = usernameMemo,
    watcherMemo = watcherMemo)

  lazy val ai: Ai = craftyAi

  lazy val craftyAi = new CraftyAi(
    execPath = config getString "crafty.exec_path",
    bookPath = Some(config getString "crafty.book_path") filter ("" !=))


  lazy val gameRepo = new GameRepo(
    mongodb(config getString "mongo.collection.game"))

  lazy val userRepo = new UserRepo(
    mongodb(config getString "mongo.collection.user"))

  lazy val hookRepo = new HookRepo(
    mongodb(config getString "mongo.collection.hook"))

  lazy val mongodb = MongoConnection(
    config getString "mongo.host",
    config getInt "mongo.port"
  )(config getString "mongo.dbName")

  lazy val versionMemo = new VersionMemo(
    getPlayer = gameRepo.playerOnly,
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

  def getMilliseconds(name: String): Int = (config getMilliseconds name).toInt
}

object SystemEnv extends EnvBuilder {

  def apply(overrides: String = "") = new SystemEnv(
    makeConfig(overrides, "/home/thib/lila/conf/application.conf")
  )
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
