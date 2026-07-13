package lila.event

import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.autoconfig.given
import lila.core.config.{ CollName, RouteUrl }

final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    cacheApi: lila.memo.CacheApi,
    markdownCache: lila.memo.MarkdownCache,
    langList: lila.core.i18n.LangList,
    ircApi: lila.irc.IrcApi,
    routeUrl: RouteUrl
)(using Executor, Scheduler):

  private lazy val eventColl = db(appConfig.get[CollName]("event.collection.event"))

  val form = wire[EventForm]

  lazy val api = wire[EventApi]

  lazy val markdown = wire[EventMarkdown]

  lazy val jsonView = wire[JsonView]
