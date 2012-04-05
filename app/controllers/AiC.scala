package controllers

import lila.http._
import DataForm._

import play.api._
import mvc._

import play.api.libs.concurrent.Akka
import play.api.Play.current

import scalaz.effects.IO

object AiC extends LilaController {

  private val craftyServer = env.craftyServer

  def run = Action { implicit request ⇒
    Async {
      Akka.future {
        craftyServer(fen = getOr("fen", ""), level = getIntOr("level", 1))
      } map { res ⇒
        res.fold(
          err ⇒ BadRequest(err.list mkString "\n"),
          op ⇒ Ok(op.unsafePerformIO)
        )
      }
    }
  }
}
