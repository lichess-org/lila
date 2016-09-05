package lila.pref

import com.typesafe.config.Config
import lila.common.PimpedConfig._

final class Env(
    config: Config,
    bus: lila.common.Bus,
    db: lila.db.Env) {

  private val CollectionPref = config getString "collection.pref"
  private val CacheTtl = config duration "cache.ttl"

  lazy val api = new PrefApi(db(CollectionPref), CacheTtl, bus)
}

object Env {

  lazy val current = "pref" boot new Env(
    config = lila.common.PlayApp loadConfig "pref",
    bus = lila.common.PlayApp.system.lilaBus,
    db = lila.db.Env.current)
}
