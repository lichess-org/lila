package controllers

import lila._
import views._

import play.api.data._
import play.api.data.Forms._
import play.api.templates._
import play.api.mvc._
import play.api.mvc.Results._
import ornicar.scalalib.OrnicarRandom

object Auth extends LilaController {

  def userRepo = env.user.userRepo
  def store = env.securityStore

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

  def gotoLoginSucceeded[A](username: String)(implicit req: RequestHeader) = {
    val sessionId = OrnicarRandom nextAsciiString 16
    store.save(sessionId, username, req)
    loginSucceeded(req).withSession("sessionId" -> sessionId)
  }

  def gotoLogoutSucceeded(implicit req: RequestHeader) = {
    req.session.get("sessionId") foreach store.delete
    logoutSucceeded(req).withNewSession
  }
}
