package lila.coach

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env) {

  private val settings = new {
    val CollectionStat = config getString "collection.stat"
  }
  import settings._

  lazy val statApi = new StatApi(coll = db(CollectionStat))
}

object Env {

  lazy val current: Env = "[boot] coach" describes new Env(
    config = lila.common.PlayApp loadConfig "coach",
    db = lila.db.Env.current)
}
