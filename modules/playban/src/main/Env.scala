package lila.playban

import akka.actor.{ ActorSelection, ActorSystem }
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env) {

  private val settings = new {
    val CollectionPlayban = config getString "collection.playban"
  }
  import settings._

  lazy val api = new PlaybanApi(coll = coll)

  private lazy val coll = db(CollectionPlayban)
}

object Env {

  lazy val current: Env = "[boot] playban" describes new Env(
    config = lila.common.PlayApp loadConfig "playban",
    db = lila.db.Env.current)
}
