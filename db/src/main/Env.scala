package lila.db

import com.typesafe.config.Config
import reactivemongo.api.DB
import play.modules.reactivemongo.ReactiveMongoPlugin

final class Env(config: Config) {

  lazy val db = {
    import play.api.Play.current
    ReactiveMongoPlugin.db
  }

  def apply(name: String): ReactiveColl = db(name)
}

object Env {

  lazy val current = new Env(lila.common.PlayApp loadConfig "mongodb")
}
