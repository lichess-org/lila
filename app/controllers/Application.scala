package controllers

import lila.http._

import play.api._
import play.api.mvc._

object Application extends Controller {

  val env = new HttpEnv(Play.unsafeApplication.configuration.underlying)

  def move(fullId: String) = Action { implicit request ⇒
    (for {
      move ← LilaForm.move.bindFromRequest.value toValid "Invalid move"
      _ ← env.server.play(fullId, move).unsafePerformIO
    } yield ()).fold(
      e ⇒ BadRequest(e.list mkString "\n"),
      a ⇒ Ok("ok")
    )
  }

  def index = TODO
}
