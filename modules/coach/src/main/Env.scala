package lila.coach

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    getPref: String => Fu[lila.pref.Pref],
    areFriends: (String, String) => Fu[Boolean],
    db: lila.db.Env) {

  private val settings = new {
    val CollectionStat = config getString "collection.stat"
  }
  import settings._

  lazy val share = new Share(getPref, areFriends)

  lazy val jsonView = new JsonView

  lazy val statApi = new StatApi(coll = db(CollectionStat))
}

object Env {

  lazy val current: Env = "[boot] coach" describes new Env(
    config = lila.common.PlayApp loadConfig "coach",
    getPref = lila.pref.Env.current.api.getPrefById,
    areFriends = lila.relation.Env.current.api.areFriends,
    db = lila.db.Env.current)
}
