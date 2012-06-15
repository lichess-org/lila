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
  def forms = env.security.forms

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
    val (form, captcha) = forms.signupWithCaptcha
    Ok(html.auth.signup(form, captcha))
  }

  def signupPost = OpenBody { implicit ctx ⇒
    implicit val req = ctx.body
    forms.signup.bindFromRequest.fold(
      err ⇒ BadRequest(html.auth.signup(err, forms.captchaCreate)),
      data ⇒ Firewall {
        val user = userRepo.create(data.username, data.password).unsafePerformIO
        gotoSignupSucceeded(
          user.err("register error").username
        )
      }
    )
  }
}
