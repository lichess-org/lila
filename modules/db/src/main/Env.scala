package lila.db

import play.api.ConfigLoader
import play.api.inject.ApplicationLifecycle
import reactivemongo.api._

final class Env(lifecycle: ApplicationLifecycle) {

  private lazy val driver = new MongoDriver()

  def connectToDb(name: String, uri: MongoConnection.ParsedURI) = new Db(
    name = name,
    uri = uri,
    driver = driver,
    lifecycle = lifecycle
  )
}

object DbConfig {

  implicit val uriLoader = ConfigLoader { c => k =>
    MongoConnection.parseURI(c.getString(k)).get
  }
}
