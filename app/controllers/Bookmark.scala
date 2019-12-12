package controllers

import lila.app._

object Bookmark extends LilaController {

  private def api = Env.bookmark.api

  def toggle(gameId: String) = Auth { implicit ctx => me =>
    api.toggle(gameId, me.id)
  }
}
