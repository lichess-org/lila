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
      IOk {
        for {
          exists ← gameRepo exists gameId
          _ ← exists.fold(starRepo.toggle(gameId, me.id), io())
        } yield ()
      }
  }
}
