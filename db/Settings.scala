package lila.db

import lila.common.ConfigSettings
import com.typesafe.config.Config

final class Settings(config: Config) extends ConfigSettings(config getObject "mongodb") {

  val MongoHost = getString("host")
  val MongoPort = getInt("port")
  val MongoDbName = getString("dbName")
  val MongoConnectionsPerHost = getInt("connectionsPerHost")
  val MongoAutoConnectRetry = getBoolean("autoConnectRetry")
  val MongoConnectTimeout = millis("connectTimeout")
  val MongoBlockingThreads = getInt("threadsAllowedToBlockForConnectionMultiplier")
}
