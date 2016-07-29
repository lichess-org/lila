package lila.db

import com.typesafe.config.Config
import dsl._
import reactivemongo.api._
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.util.{ Failure, Success, Try }

final class Env(
    name: String,
    config: Config,
    lifecycle: play.api.inject.ApplicationLifecycle) {

  private val configUri = config getString "uri"

  logger.info(s"Instanciate db env for $name, uri: $configUri")

  lazy val (connection, dbName) = {
    val driver = new MongoDriver(Some(config))

    registerDriverShutdownHook(driver)

    (for {
      parsedUri <- MongoConnection.parseURI(configUri)
      con <- driver.connection(parsedUri, true)
      db <- parsedUri.db match {
        case Some(name) => Success(name)
        case _ => Failure[String](new IllegalArgumentException(
          s"cannot resolve database from URI: $parsedUri"))
      }
    } yield con -> db).get
  }

  @inline private def resolveDB = connection.database(dbName).andThen {
    case _ =>
      logger.info(s"ReactiveMongoApi successfully started with DB '$dbName'!")
  }

  def db: DefaultDB = Await.result(resolveDB, 10.seconds)

  def apply(name: String): Coll = db(name)

  private def registerDriverShutdownHook(mongoDriver: MongoDriver): Unit =
    lifecycle.addStopHook { () => Future(mongoDriver.close()) }
}

object Env {

  lazy val current = "db" boot new Env(
    name = "main",
    config = lila.common.PlayApp loadConfig "mongodb",
    lifecycle = lila.common.PlayApp.lifecycle)
}
