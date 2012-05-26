package controllers

import lila._
import views._

import play.api.data._
import play.api.data.Forms._
import play.api.templates._
import play.api.mvc._
import play.api.mvc.Results._

object Auth extends LilaController {

  def userRepo = env.user.userRepo

  def login = Action { implicit req ⇒
    Ok(html.auth.login(loginForm))
  }

  def logout = Action { implicit req ⇒
    gotoLogoutSucceeded
  }

  def authenticate = Action { implicit req ⇒
    loginForm.bindFromRequest.fold(
      formWithErrors ⇒ BadRequest(html.auth.login(formWithErrors)),
      _.fold(
        user ⇒ gotoLoginSucceeded(user.username),
        BadRequest("wtf")
      )
    )
  }
}
