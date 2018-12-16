package controllers

import play.api.libs.json._

import lila.app._
import lila.common.HTTPRequest
import lila.timeline.Entry.entryWrites
import views._

object Timeline extends LilaController {

  def home = Auth { implicit ctx => me =>
    def nb = getInt("nb").fold(15)(_ min 50)
    lila.mon.http.response.timeline.count()
    negotiate(
      html =
        if (HTTPRequest.isXhr(ctx.req))
          Env.timeline.entryApi.userEntries(me.id) map { html.timeline.entries(_) }
        else
          Env.timeline.entryApi.moreUserEntries(me.id, nb) map { html.timeline.more(_) },
      _ => Env.timeline.entryApi.moreUserEntries(me.id, nb) map { es => Ok(Json.obj("entries" -> es)) }
    ).mon(_.http.response.accountInfo.time)
  }

  def unsub(channel: String) = Auth { implicit ctx => me =>
    Env.timeline.unsubApi.set(channel, me.id, ~get("unsub") == "on")
  }
}
