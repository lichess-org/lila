package lila.db

import Types._

import com.typesafe.config.Config
import reactivemongo.api.DB
import play.modules.reactivemongo.ReactiveMongoPlugin

final class Env(config: Config) {

  lazy val db = {
    import play.api.Play.current
    ReactiveMongoPlugin.db
  }

  def apply(name: String): Coll = db(name)
}

object Env {

  lazy val current = "[boot] db" describes new Env(
    lila.common.PlayApp loadConfig "mongodb")
}
