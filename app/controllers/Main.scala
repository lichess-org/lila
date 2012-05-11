package controllers

import lila._
import views._
import security.AuthConfigImpl

import jp.t2v.lab.play20.auth.LoginLogout

import play.api._
import mvc._
import Results._

import play.api.libs.concurrent.Akka
import play.api.Play.current

object Main extends LilaController with LoginLogout with AuthConfigImpl {

  val home = Open { implicit user => implicit request ⇒
    Ok(html.home(user))
  }
}
