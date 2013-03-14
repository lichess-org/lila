package controllers

import lila.app._
import views._

import play.api.mvc._
import scalaz.effects._

object Bookmark extends LilaController {

  private def api = env.bookmark.api
  private def gameRepo = env.game.gameRepo

  def toggle(gameId: String) = Auth { implicit ctx ⇒
    me ⇒ IOk(api.toggle(gameId, me.id))
  }
}
