package lila.event

import play.api.Configuration
import com.softwaremill.macwire.*

import lila.common.config.CollName
import lila.common.autoconfig.given

final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  private lazy val eventColl = db(appConfig.get[CollName]("event.collection.event"))

  lazy val api = wire[EventApi]
