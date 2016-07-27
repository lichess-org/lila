package lila.db

import com.typesafe.config.Config
import reactivemongo.api._
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.util.{ Failure, Success, Try }
import dsl._

final class Env(
    config: Config,
    lifecycle: play.api.inject.ApplicationLifecycle) {

  lazy val db = {
    val driver = new MongoDriver(Some(config))

    registerDriverShutdownHook(driver)

    (for {
      parsedUri <- MongoConnection.parseURI(config getString "uri")
      con <- driver.connection(parsedUri, true)
      db <- parsedUri.db match {
        case Some(name) => {
          def resolvedDB = con.database(name).andThen {
            case _ =>
              logger.info(s"""ReactiveMongoApi successfully started with DB '$name'! Servers: ${parsedUri.hosts.map { s => s"[${s._1}:${s._2}]" }.mkString("\n\t\t")}""")
          }

          Try(Await.result(resolvedDB, 10.seconds))
        }

        case _ => {
          Failure[DefaultDB](new IllegalArgumentException(
            s"cannot resolve database from URI: $parsedUri"))
        }
      }
    } yield db).get
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
