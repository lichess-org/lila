package lila.db

import com.typesafe.config.Config
import reactivemongo.api.{ DefaultDB, MongoConnection, MongoDriver }
import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }
import dsl.Coll

final class Env(
    name: String,
    config: Config,
    lifecycle: play.api.inject.ApplicationLifecycle) {

  lazy val (connection, dbName) = {
    val driver = new MongoDriver(Some(config))

    registerDriverShutdownHook(driver)

    (for {
      parsedUri <- MongoConnection.parseURI(config getString "uri")
      con <- driver.connection(parsedUri, true)
      db <- parsedUri.db match {
        case Some(name) => Success(name)
        case _ => Failure[String](new IllegalArgumentException(
          s"cannot resolve connection from URI: $parsedUri"))
      }
    } yield con -> db).get
  }

  private lazy val lnm = s"${connection.supervisor}/${connection.name}"

  @inline private def resolveDB(ec: ExecutionContext) =
    connection.database(dbName)(ec).andThen {
      case _ => /*logger.debug*/println(s"[$lnm] MongoDB resolved: $dbName")
    }

  def db(implicit ec: ExecutionContext): DefaultDB =
    Await.result(resolveDB(ec), 10.seconds)

  def apply(name: String)(implicit ec: ExecutionContext): Coll =
    db(ec).apply(name)

  private def registerDriverShutdownHook(mongoDriver: MongoDriver): Unit =
    lifecycle.addStopHook { () =>
      logger.info(s"[$lnm] Stopping the MongoDriver...")
      Future(mongoDriver.close())
    }
}

object Env {

  lazy val current = "db" boot new Env(
    name = "main",
    config = lila.common.PlayApp loadConfig "mongodb",
    lifecycle = lila.common.PlayApp.lifecycle)
}
