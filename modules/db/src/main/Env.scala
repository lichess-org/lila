package lila.db

import akka.actor.CoordinatedShutdown
import com.typesafe.config.Config
import play.api.{ ConfigLoader, Configuration }
import reactivemongo.api._

final class Env(
    appConfig: Configuration,
    shutdown: CoordinatedShutdown
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val driver = new AsyncDriver(appConfig.get[Config]("mongodb").some)

  def asyncDb(name: String, uri: MongoConnection.ParsedURI) = new AsyncDb(
    name = name,
    uri = uri,
    driver = driver
  )

  def blockingDb(name: String, uri: MongoConnection.ParsedURI) = new Db(
    name = name,
    uri = uri,
    driver = driver
  )

  shutdown.addTask(CoordinatedShutdown.PhaseServiceStop, "Closing mongodb driver") { () =>
    driver.close() inject akka.Done
  }
}

object DbConfig {

  implicit val uriLoader = ConfigLoader { c => k =>
    MongoConnection.parseURI(c.getString(k)).get
  }
}
