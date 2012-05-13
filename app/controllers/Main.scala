package controllers

import lila._
import views._
import security.AuthConfigImpl

import jp.t2v.lab.play20.auth.LoginLogout

import play.api._
import play.api.mvc._
import play.api.mvc.Results._

object Main extends LilaController with LoginLogout with AuthConfigImpl {

  val home = Open { implicit me ⇒
    implicit req ⇒
      Ok(html.home())
  }
}
