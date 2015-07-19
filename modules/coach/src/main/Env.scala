package lila.coach

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import akka.actor._
import lila.common.PimpedConfig._

final class Env(
    config: Config,
    getPref: String => Fu[lila.pref.Pref],
    areFriends: (String, String) => Fu[Boolean],
    system: ActorSystem,
    db: lila.db.Env) {

  private val settings = new {
    val CollectionStat = config getString "collection.stat"
  }
  import settings._

  lazy val share = new Share(getPref, areFriends)

  lazy val jsonView = new JsonView

  lazy val statApi = new StatApi(
    coll = db(CollectionStat),
    makeThrottler = f => new Throttler(system, f))
}

object Env {

  lazy val current: Env = "[boot] coach" describes new Env(
    config = lila.common.PlayApp loadConfig "coach",
    getPref = lila.pref.Env.current.api.getPrefById,
    areFriends = lila.relation.Env.current.api.areFriends,
    system = lila.common.PlayApp.system,
    db = lila.db.Env.current)
}
