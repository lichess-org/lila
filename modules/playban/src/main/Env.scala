package lila.playban

import akka.actor.{ ActorSelection, ActorSystem }
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    isRematch: String => Boolean,
    db: lila.db.Env) {

  private val settings = new {
    val CollectionPlayban = config getString "collection.playban"
  }
  import settings._

  lazy val api = new PlaybanApi(coll = coll, isRematch = isRematch)

  private lazy val coll = db(CollectionPlayban)
}

object Env {

  lazy val current: Env = "playban" boot new Env(
    config = lila.common.PlayApp loadConfig "playban",
    isRematch = lila.game.Env.current.cached.isRematch.get _,
    db = lila.db.Env.current)
}
