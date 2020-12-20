package lila.event

import play.api.Configuration
import com.softwaremill.macwire._

import lila.common.config.CollName
import lila.common.config._

final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    cacheApi: lila.memo.CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  private lazy val eventColl = db(appConfig.get[CollName]("event.collection.event"))

  lazy val api = wire[EventApi]
}
