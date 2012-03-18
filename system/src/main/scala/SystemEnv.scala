package lila.system

import com.mongodb.casbah.MongoConnection
import com.mongodb.casbah.commons.conversions.scala._
import com.typesafe.config._

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
    duration = config getInt "sync.duration",
    sleep = config getInt "sync.sleep")

  lazy val ai: Ai = new CraftyAi

  def gameRepo = new GameRepo(
    mongodb(config getString "mongo.collection.game"))

  def mongodb = MongoConnection(
    config getString "mongo.host",
    config getInt "mongo.port"
  )(config getString "mongo.dbName")

  lazy val versionMemo = new VersionMemo(
    repo = gameRepo,
    timeout = config getInt "memo.version.timeout")

  lazy val aliveMemo = new AliveMemo(
    hardTimeout = config getInt "memo.alive.hard_timeout",
    softTimeout = config getInt "memo.alive.soft_timeout")
}

object SystemEnv extends EnvBuilder {

  def apply(overrides: String = "") = new SystemEnv {
    val config = makeConfig(overrides, "/home/thib/lila/conf/application.conf")
  }

  def apply(c: Config) = new SystemEnv {
    val config = c
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
