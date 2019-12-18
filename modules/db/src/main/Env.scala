package lila.db

import com.typesafe.config.Config
import play.api.inject.ApplicationLifecycle
import play.api.{ ConfigLoader, Configuration }
import reactivemongo.api._

final class Env(
    appConfig: Configuration,
    lifecycle: ApplicationLifecycle
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

  lifecycle.addStopHook { () =>
    println("close driver")
    driver.close()
  }
}

object DbConfig {

  implicit val uriLoader = ConfigLoader { c => k =>
    MongoConnection.parseURI(c.getString(k)).get
  }
}
