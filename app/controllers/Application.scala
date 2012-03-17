package controllers

import lila.http._
import DataForm._

import play.api._
import play.api.mvc._
import com.codahale.jerkson.Json.{ generate => jsonify }

object Application extends Controller {

  val env = new HttpEnv(Play.unsafeApplication.configuration.underlying)
  val json = "application/json"

  def sync(id: String, color: String, version: Int, fullId: String) = Action {
    Ok(jsonify(
      env.syncer.sync(id, color, version, fullId).unsafePerformIO
    )) as json
  }

  def move(fullId: String) = Action { implicit request ⇒
    (moveForm.bindFromRequest.value toValid "Invalid move" flatMap { move =>
      env.server.play(fullId, move._1, move._2, move._3).unsafePerformIO
    }).fold(
      e ⇒ BadRequest(e.list mkString "\n"),
      _ ⇒ Ok("ok")
    )
  }

  def index = TODO
}
