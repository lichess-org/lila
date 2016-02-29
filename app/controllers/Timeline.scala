package controllers

import play.api.libs.json._
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.common.HTTPRequest
import views._

object Timeline extends LilaController {

  def home = Auth { implicit ctx =>
    me =>
      if (HTTPRequest.isXhr(ctx.req))
        Env.timeline.entryRepo.userEntries(me.id) map { html.timeline.entries(_) }
      else negotiate(
        html = Env.timeline.entryRepo.moreUserEntries(me.id) map { html.timeline.more(_) },
        _ => ???)
  }

  def unsub(channel: String) = Auth { implicit ctx =>
    me =>
      Env.timeline.unsubApi.set(channel, me.id, ~get("unsub") == "on")
  }
}
