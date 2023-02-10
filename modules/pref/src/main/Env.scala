package lila.pref

import com.softwaremill.macwire.Module

import lila.common.config.CollName

@Module
final class Env(
    cacheApi: lila.memo.CacheApi,
    db: lila.db.Db
)(using Executor):

  val api = new PrefApi(db(CollName("pref")), cacheApi)
