package controllers

import play.api.libs.json._
import views._

import lila.app._
import lila.common.config.Max
import lila.common.HTTPRequest
import lila.timeline.Entry.entryWrites

final class Timeline(env: Env) extends LilaController(env) {

  def home =
    Auth { implicit ctx => me =>
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
            entries <- env.timeline.entryApi
              .moreUserEntries(
                me.id,
                Max(getInt("nb") | 10) atMost env.apiTimelineSetting.get()
              )
            users <- env.user.lightUserApi.asyncManyFallback(entries.flatMap(_.userIds).distinct)
            userMap = users.view.map { u => u.id -> u }.toMap
          } yield Ok(Json.obj("entries" -> entries, "users" -> userMap))
      )
    }

  def unsub(channel: String) =
    Auth { implicit ctx => me =>
      env.timeline.unsubApi.set(channel, me.id, ~get("unsub") == "on")
    }
}
