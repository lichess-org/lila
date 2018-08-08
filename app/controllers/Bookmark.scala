package controllers

import lidraughts.app._

object Bookmark extends LidraughtsController {

  private def api = Env.bookmark.api

  def toggle(gameId: String) = Auth { implicit ctx => me => api.toggle(gameId, me.id)
  }
}
