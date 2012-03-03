package lila.system

import com.mongodb.casbah.MongoConnection
import com.mongodb.casbah.commons.conversions.scala._
import com.redis.RedisClient
import com.typesafe.config._

trait SystemEnv {

  val config: Config

  def server = new Server(
    repo = gameRepo)

  def gameRepo = new GameRepo(
    mongodb(config getString "mongo.collection.game"))

  def mongodb = MongoConnection(
    config getString "mongo.host",
    config getInt "mongo.port"
  )(config getString "mongo.dbName")

  def redis = new RedisClient(
    config getString "redis.host",
    config getInt "redis.port")
}

object SystemEnv extends EnvBuilder {

  def apply(overrides: String = "") = new SystemEnv {
    val config = makeConfig(overrides, "/home/thib/lila/lila.conf")
  }
}

trait EnvBuilder {

  import java.io.File

  def makeConfig(sources: String*) = sources.foldLeft(ConfigFactory.defaultOverrides) {
    case (config, source) if source contains '=' ⇒
      config.withFallback(ConfigFactory parseString source)
    case (config, source) ⇒
      config.withFallback(ConfigFactory parseFile (new File(source)))
  }
}
