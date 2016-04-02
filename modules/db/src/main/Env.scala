package lila.db

import com.typesafe.config.Config
import reactivemongo.api._
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{ Success, Failure }
import dsl._

final class Env(
    config: Config,
    lifecycle: play.api.inject.ApplicationLifecycle) {

  lazy val db = {
    val parsedUri: MongoConnection.ParsedURI =
      MongoConnection.parseURI(config.getString("uri")) match {
        case Success(parsedURI) => parsedURI
        case Failure(e)         => sys error s"Invalid mongodb.uri"
      }
    val driver = new MongoDriver(Some(config))
    val connection = driver.connection(parsedUri)

    parsedUri.db.fold[DefaultDB](sys error s"cannot resolve database from URI: $parsedUri") { dbUri =>
      val db = DB(dbUri, connection)
      registerDriverShutdownHook(driver)
      logger.info(s"""ReactiveMongoApi successfully started with DB '$dbUri'! Servers: ${parsedUri.hosts.map { s => s"[${s._1}:${s._2}]" }.mkString("\n\t\t")}""")
      db
    }
  }

  def apply(name: String): Coll = db(name)

  private def registerDriverShutdownHook(mongoDriver: MongoDriver): Unit =
    lifecycle.addStopHook { () => Future(mongoDriver.close()) }
}

object Env {

  lazy val current = "db" boot new Env(
    config = lila.common.PlayApp loadConfig "mongodb",
    lifecycle = lila.common.PlayApp.lifecycle)
}
