package controllers

import lila._
import views._

import play.api.mvc._
import scalaz.effects._

object Star extends LilaController {

  val starRepo = env.star.starRepo
  val gameRepo = env.game.gameRepo

  def toggle(gameId: String) = Auth { implicit ctx ⇒
    me ⇒
      IOptionIOk(gameRepo game gameId) { game ⇒
        starRepo.toggle(game.id, me.id)
      }
  }
}
