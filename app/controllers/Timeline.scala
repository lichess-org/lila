package controllers

import play.api.libs.json._
import scala.concurrent.duration._

import lila.app._
import lila.common.config.Max
import lila.common.HTTPRequest
import lila.timeline.Entry.entryWrites
import views._

final class Timeline(env: Env) extends LilaController(env) {

  def home =
    Auth { implicit ctx => me =>
      negotiate(
        html =
          if (HTTPRequest.isXhr(ctx.req))
            env.timeline.entryApi
              .userEntries(me.id, ctx.lang.code)
              .logTimeIfGt(s"timeline site entries for ${me.id}", 10.seconds)
              .map { html.timeline.entries(_) }
          else
            env.timeline.entryApi
              .moreUserEntries(me.id, Max(30), ctx.lang.code)
              .map { html.timeline.more(_) },
        _ =>
          env.timeline.entryApi
            .moreUserEntries(me.id, Max(getInt("nb") | 10) atMost env.apiTimelineSetting.get(), ctx.lang.code)
            .map { es =>
              Ok(Json.obj("entries" -> es))
            }
      )
    }

  def unsub(channel: String) =
    Auth { implicit ctx => me =>
      env.timeline.unsubApi.set(channel, me.id, ~get("unsub") == "on")
    }
}
