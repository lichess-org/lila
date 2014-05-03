package controllers

import play.api.mvc._

import lila.app._
import views._

object Coordinate extends LilaController {

  private def env = Env.coordinate

  def home = Open { implicit ctx =>
    fuccess(views.html.coordinate.home())
  }
}
