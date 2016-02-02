package controllers

import lila.app._
import views._

object Explorer extends LilaController {

  def home = Open { implicit ctx =>
    fuccess(Ok(views.html.explorer.home()))
  }

}
