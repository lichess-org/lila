package controllers

import lila._
import views._

import play.api._
import play.api.mvc._
import play.api.mvc.Results._

object User extends LilaController {

  def show(username: String) = Open { implicit me ⇒
    implicit req ⇒
      Ok(html.home())
  }

  val signUp = Open { implicit me ⇒
    implicit req ⇒
      Ok(html.home())
  }
}
