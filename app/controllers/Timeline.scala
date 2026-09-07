package controllers

import play.api.libs.json.*

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.common.Json.given
import lila.timeline.Entry.given

final class Timeline(env: Env) extends LilaController(env):

  def home = Auth { ctx ?=> me ?=>
    negotiate(
      html =
        if HTTPRequest.isXhr(ctx.req) then
          for
            entries <- env.timeline.entryApi.userEntries(me)
            _ <- env.user.lightUserApi.preloadMany(entries.flatMap(_.userIds))
          yield Ok.snip(views.timeline.entries(entries))
        else
          for
            entries <- env.timeline.entryApi.moreUserEntries(me, Max(30), since = getTimestamp("since"))
            _ <- env.user.lightUserApi.preloadMany(entries.flatMap(_.userIds))
            res <- Ok.page(views.timeline.more(entries))
          yield res
      ,
      json =
        // Must be empty if nb is not given, because old versions of the
        // mobile app that do not send nb are vulnerable to XSS in
        // timeline entries.
        apiOutput(max = getIntAs[Max]("nb").fold(Max(0))(_.atMost(env.web.settings.apiTimeline.get())))
    )
  }

  def api = AuthOrScoped():
    apiOutput(getIntAs[Max]("nb").fold(Max(15))(_.atMost(Max(30))))

  private def apiOutput(max: Max)(using ctx: Context, me: Me) = for
    entries <- env.timeline.entryApi.moreUserEntries(me, max, since = getTimestamp("since"))
    users <- env.user.lightUserApi.asyncManyFallback(entries.flatMap(_.userIds).distinct)
    userMap = users.mapBy(_.id)
  yield Ok(Json.obj("entries" -> entries, "users" -> Json.toJsObject(userMap)))

  def unsub(channel: String) = Auth { ctx ?=> me ?=>
    env.timeline.unsubApi.set(channel, me, ~get("unsub") == "on").inject(NoContent)
  }
