package lila.db

import akka.actor.CoordinatedShutdown
import com.softwaremill.macwire._
import com.softwaremill.tagging._
import com.typesafe.config.Config
import play.api.Configuration
import reactivemongo.api._
import scala.concurrent.ExecutionContext

import lila.common.Lilakka

trait JunkDb

@Module
final class Env(
    appConfig: Configuration,
    shutdown: CoordinatedShutdown
)(implicit ec: ExecutionContext) {

  private val driver = new AsyncDriver(appConfig.get[Config]("mongodb").some)

  lazy val mainDb = new Db(
    name = "main",
    uri = appConfig.get[String]("mongodb.uri"),
    driver = driver
  )

  lazy val junkDb = new AsyncDb(
    name = "junk",
    uri = appConfig.get[String]("mongodb.junk.uri"),
    driver = driver
  ).taggedWith[JunkDb]

  def asyncDb(name: String, uri: String) =
    new AsyncDb(
      name = name,
      uri = uri,
      driver = driver
    )

  Lilakka.shutdown(shutdown, _.PhaseServiceStop, "Closing mongodb driver") { () =>
    driver.close()
  }
}
