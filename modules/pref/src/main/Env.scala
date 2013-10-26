package lila.pref

import com.typesafe.config.Config
import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env) {

  private val CollectionPref = config getString "collection.pref"
  private val CacheTtl = config duration "cache.ttl"

  def forms = new DataForm(api)

  lazy val api = new PrefApi(CacheTtl)

  private[pref] lazy val prefColl = db(CollectionPref)
}

object Env {

  lazy val current = "[boot] pref" describes new Env(
    config = lila.common.PlayApp loadConfig "pref",
    db = lila.db.Env.current)
}
