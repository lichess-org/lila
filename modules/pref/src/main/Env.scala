package lila.pref

import com.typesafe.config.Config
import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env) {

  private val CollectionPref = config getString "collection.pref"
  private val CacheTtl = config duration "cache.ttl"

  lazy val api = new PrefApi(db(CollectionPref), CacheTtl)
}

object Env {

  lazy val current = "pref" boot new Env(
    config = lila.common.PlayApp loadConfig "pref",
    db = lila.db.Env.current)
}
