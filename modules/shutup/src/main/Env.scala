package lila.shutup

import akka.actor.{ ActorSelection, ActorSystem }
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    reporter: akka.actor.ActorSelection,
    db: lila.db.Env) {

  private val settings = new {
    val CollectionShutup = config getString "collection.shutup"
  }
  import settings._

  lazy val api = new ShutupApi(
    coll = coll,
    reporter = reporter)

  private lazy val coll = db(CollectionShutup)
}

object Env {

  lazy val current: Env = "[boot] shutup" describes new Env(
    config = lila.common.PlayApp loadConfig "shutup",
    reporter = lila.hub.Env.current.actor.report,
    db = lila.db.Env.current)
}
