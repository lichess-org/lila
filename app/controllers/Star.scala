package controllers

import lila._
import views._

import play.api.mvc._
import scalaz.effects._

object Bookmark extends LilaController {

  val api = env.bookmark.api
  val gameRepo = env.game.gameRepo

  def toggle(gameId: String) = Auth { implicit ctx ⇒
    me ⇒
      IOk(api.toggle(gameId, me)) 
  }
}
