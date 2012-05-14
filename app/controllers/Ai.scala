package controllers

import lila._
import http.Context

import play.api._
import mvc._

import play.api.libs.concurrent.Akka
import play.api.Play.current

object Ai extends LilaController {

  private val craftyServer = env.ai.craftyServer

  def run = Action { implicit req ⇒
    implicit val ctx = Context(req, None)
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
