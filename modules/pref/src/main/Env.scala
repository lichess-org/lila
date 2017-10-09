package lila.pref

import com.typesafe.config.Config

final class Env(
    config: Config,
    asyncCache: lila.memo.AsyncCache.Builder,
    db: lila.db.Env
) {

  private val CollectionPref = config getString "collection.pref"
  private val CacheTtl = config duration "cache.ttl"

  lazy val api = new PrefApi(db(CollectionPref), asyncCache, CacheTtl)
}

object Env {

  lazy val current = "pref" boot new Env(
    config = lila.common.PlayApp loadConfig "pref",
    asyncCache = lila.memo.Env.current.asyncCache,
    db = lila.db.Env.current
  )
}
