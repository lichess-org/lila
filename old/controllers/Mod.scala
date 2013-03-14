package controllers

import lila.app._
import views._
import security.Permission
import http.Context

import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.concurrent.Execution.Implicits._

object Mod extends LilaController {

  private def modApi = env.mod.api
  private def modLogApi = env.mod.logApi

  def engine(username: String) = Secure(Permission.MarkEngine) { _ ⇒
    me ⇒ AsyncRedirect(modApi.adjust(me, username))(routes.User show username)
  }

  def mute(username: String) = Secure(Permission.MutePlayer) { _ ⇒
    me ⇒ AsyncRedirect(modApi.mute(me, username))(routes.User show username)
  }

  def ban(username: String) = Secure(Permission.IpBan) { implicit ctx ⇒
    me ⇒ AsyncRedirect(modApi.ban(me, username))(routes.User show username)
  }

  def ipban(ip: String) = Secure(Permission.IpBan) { implicit ctx ⇒
    me ⇒ AsyncUnit(modApi.ipban(me, ip))
  }

  val log = Auth { implicit ctx ⇒
    me ⇒ AsyncOk(modLogApi.recent) { html.mod.log(_) }
  }
}
