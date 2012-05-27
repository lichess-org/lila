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

  def login = Open { implicit ctx ⇒
    Ok(html.auth.login(loginForm))
  }

  def authenticate = OpenBody { implicit ctx ⇒
    implicit val req = ctx.body
    loginForm.bindFromRequest.fold(
      err ⇒ BadRequest(html.auth.login(err)),
      userOption ⇒ gotoLoginSucceeded(
        userOption.err("authenticate error").username
      )
    )
  }

  def logout = Open { implicit ctx ⇒
    gotoLogoutSucceeded(ctx.req)
  }

  def signup = Open { implicit ctx ⇒
    Ok(html.auth.signup(signupForm))
  }

  def signupPost = OpenBody { implicit ctx ⇒
    implicit val req = ctx.body
    signupForm.bindFromRequest.fold(
      err ⇒ BadRequest(html.auth.signup(err)),
      userOption ⇒ gotoSignupSucceeded(
        userOption.err("authenticate error").username
      )
    )
  }
}
