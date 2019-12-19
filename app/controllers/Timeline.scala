package controllers

import play.api.libs.json._
import scala.concurrent.duration._

import lila.app._
import lila.common.config.Max
import lila.common.HTTPRequest
import lila.timeline.Entry.entryWrites
import views._

final class Timeline(env: Env) extends LilaController(env) {

  def home = Auth { implicit ctx => me =>
    def nb = Max(getInt("nb").fold(15)(_ min 50))
    negotiate(
      html =
        if (HTTPRequest.isXhr(ctx.req))
          env.timeline.entryApi
            .userEntries(me.id)
            .logTimeIfGt(s"timeline site entries for ${me.id}", 10 seconds)
            .map { html.timeline.entries(_) } else
          env.timeline.entryApi
            .moreUserEntries(me.id, nb)
            .logTimeIfGt(s"timeline site more entries ($nb) for ${me.id}", 10 seconds)
            .map { html.timeline.more(_) },
      _ =>
        env.timeline.entryApi
          .moreUserEntries(me.id, nb)
          .logTimeIfGt(s"timeline mobile $nb for ${me.id}", 10 seconds)
          .map { es =>
            Ok(Json.obj("entries" -> es))
          }
    )
  }

  def unsub(channel: String) = Auth { implicit ctx => me =>
    env.timeline.unsubApi.set(channel, me.id, ~get("unsub") == "on")
  }
}
