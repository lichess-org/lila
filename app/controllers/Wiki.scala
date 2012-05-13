package controllers

import lila._
import views._

object Wiki extends LilaController {

  val home = Open { implicit user ⇒
    implicit req ⇒
      Ok(html.home())
  }
}
