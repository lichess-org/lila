package lidraughts.pref

import com.typesafe.config.Config

final class Env(
    config: Config,
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    db: lidraughts.db.Env
) {

  private val CollectionPref = config getString "collection.pref"
  private val CacheTtl = config duration "cache.ttl"

  lazy val api = new PrefApi(db(CollectionPref), asyncCache, CacheTtl)
}

object Env {

  lazy val current = "pref" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "pref",
    asyncCache = lidraughts.memo.Env.current.asyncCache,
    db = lidraughts.db.Env.current
  )
}
