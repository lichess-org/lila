package controllers

import lila.http._
import DataForm._

import play.api._
import mvc._

object Application extends LilaController {

  private val syncer = env.syncer
  private val server = env.server

  def sync(id: String, color: String, version: Int, fullId: Option[String]) =
    Action {
      JsonOk(env.syncer.sync(id, color, version, fullId).unsafePerformIO)
    }

  def move(fullId: String) = Action { implicit request ⇒
    ValidOk(moveForm.bindFromRequest.toValid flatMap { move ⇒
      env.server.play(fullId, move._1, move._2, move._3).unsafePerformIO
    })
  }
}
