package controllers

import play.api.libs.json.*
import views.*

import lila.app.{ given, * }
import lila.common.config.Max
import lila.common.HTTPRequest
import lila.common.Json.given
import lila.timeline.Entry.given

final class Timeline(env: Env) extends LilaController(env):

  def home = Auth { ctx ?=> me =>
    negotiate(
      html =
        if (HTTPRequest isXhr ctx.req) for {
          entries <- env.timeline.entryApi.userEntries(me.id)
          _       <- env.user.lightUserApi.preloadMany(entries.flatMap(_.userIds))
        } yield html.timeline.entries(entries)
        else
          for {
            entries <- env.timeline.entryApi.moreUserEntries(me.id, Max(30))
            _       <- env.user.lightUserApi.preloadMany(entries.flatMap(_.userIds))
          } yield html.timeline.more(entries),
      _ =>
        for {
          // Must be empty if nb is not given, because old versions of the
          // mobile app that do not send nb are vulnerable to XSS in
          // timeline entries.
          entries <- env.timeline.entryApi
            .moreUserEntries(me.id, Max(getInt("nb") | 0 atMost env.apiTimelineSetting.get()))
          users <- env.user.lightUserApi.asyncManyFallback(entries.flatMap(_.userIds).distinct)
          userMap = users.mapBy(_.id)
        } yield Ok(Json.obj("entries" -> entries, "users" -> Json.toJsObject(userMap)))
    )
  }

  def unsub(channel: String) = Auth { ctx ?=> me =>
    env.timeline.unsubApi.set(channel, me.id, ~get("unsub") == "on")
  }
