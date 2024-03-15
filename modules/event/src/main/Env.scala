package lila.event

import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.autoconfig.given
import lila.common.config.CollName

final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  private lazy val eventColl = db(appConfig.get[CollName]("event.collection.event"))

  lazy val api = wire[EventApi]
