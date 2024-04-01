package lila.event

import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.autoconfig.given
import lila.core.config.CollName

final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    cacheApi: lila.memo.CacheApi,
    langList: lila.core.i18n.LangList
)(using Executor):

  private lazy val eventColl = db(appConfig.get[CollName]("event.collection.event"))

  val form = wire[EventForm]

  lazy val api = wire[EventApi]
