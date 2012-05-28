package lila
package mongodb

import com.mongodb.casbah.MongoConnection
import com.mongodb.{ Mongo, MongoOptions, ServerAddress ⇒ MongoServer }

import core.Settings

final class MongoDbEnv(
    settings: Settings) {

  import settings._

  def apply(coll: String) = connection(coll)

  lazy val cache = new Cache(connection(MongoCollectionCache))

  lazy val connection = MongoConnection(server, options)(MongoDbName)

  private lazy val server = new MongoServer(MongoHost, MongoPort)

  // http://stackoverflow.com/questions/6520439/how-to-configure-mongodb-java-driver-mongooptions-for-production-use
  private val options = new MongoOptions() ~ { o ⇒
    o.connectionsPerHost = MongoConnectionsPerHost
    o.autoConnectRetry = MongoAutoConnectRetry
    o.connectTimeout = MongoConnectTimeout
    o.threadsAllowedToBlockForConnectionMultiplier = MongoBlockingThreads
  }
}
