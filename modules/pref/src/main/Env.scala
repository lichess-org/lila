package lila.pref

import com.softwaremill.macwire.Module

import lila.core.config.CollName

@Module
final class Env(
    cacheApi: lila.memo.CacheApi,
    db: lila.db.Db
)(using Executor):

  val api = PrefApi(db(CollName("pref")), cacheApi)
