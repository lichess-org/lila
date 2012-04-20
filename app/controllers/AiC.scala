package lila
package controllers

import play.api._
import mvc._

import play.api.libs.concurrent.Akka
import play.api.Play.current

object AiC extends LilaController {

  private val craftyServer = env.craftyServer

  def run = Action { implicit request ⇒
    Async {
      Akka.future {
        craftyServer(fen = getOr("fen", ""), level = getIntOr("level", 1))
      } map { res ⇒
        res.fold(
          err ⇒ BadRequest(err.shows),
          op ⇒ Ok(op.unsafePerformIO)
        )
      }
    }
  }
}
