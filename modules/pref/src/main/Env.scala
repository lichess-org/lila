package lila.pref

import com.softwaremill.macwire.Module

import lila.common.config.CollName

@Module
final class Env(
    asyncCache: lila.memo.AsyncCache.Builder,
    db: lila.db.Db
)(implicit ec: scala.concurrent.ExecutionContext) {

  lazy val api = new PrefApi(db(CollName("pref")), asyncCache)
}
