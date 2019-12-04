package lila.pref

import com.softwaremill.macwire.Module
import scala.concurrent.duration._

import lila.common.config.CollName

@Module
final class Env(
    asyncCache: lila.memo.AsyncCache.Builder,
    db: lila.db.Env
) {
  lazy val api = new PrefApi(
    db(CollName("pref")),
    asyncCache,
    10 minutes
  )
}
