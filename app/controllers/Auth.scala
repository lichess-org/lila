package controllers

import lila._
import security.AuthConfigImpl

import jp.t2v.lab.play20.auth.LoginLogout
import play.api.data._
import play.api.data.Forms._
import play.api.templates._
import views._
import play.api.mvc._
import play.api.mvc.Results._

object Auth extends LilaController with LoginLogout with AuthConfigImpl {

  def login = Action { implicit req ⇒
    Ok(html.login(loginForm))
  }

  def logout = Action { implicit req ⇒
    gotoLogoutSucceeded
  }

  def authenticate = Action { implicit req ⇒
    loginForm.bindFromRequest.pp.fold(
      formWithErrors ⇒ BadRequest(html.login(formWithErrors)),
      _.fold(
        user ⇒ gotoLoginSucceeded(user.username),
        BadRequest("wtf")
      )
    )
  }
}
