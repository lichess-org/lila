package controllers

import lila.app._
import views._
import lila.security.Permission

import play.api.mvc._
import play.api.mvc.Results._

object Mod extends LilaController {

  private def modApi = Env.mod.api
  private def modLogApi = Env.mod.logApi

  def engine(username: String) = TODO
  // Secure(Permission.MarkEngine) { _ ⇒
  //   me ⇒ AsyncRedirect(modApi.adjust(me, username))(routes.User show username)
  // }

  def mute(username: String) = TODO
  // Secure(Permission.MutePlayer) { _ ⇒
  //   me ⇒ AsyncRedirect(modApi.mute(me, username))(routes.User show username)
  // }

  def ban(username: String) = TODO
  // Secure(Permission.IpBan) { implicit ctx ⇒
  //   me ⇒ AsyncRedirect(modApi.ban(me, username))(routes.User show username)
  // }

  def ipban(ip: String) = Secure(Permission.IpBan) { implicit ctx ⇒
    me ⇒ modApi.ipban(me.id, ip)
  }

  def log = TODO
  // Auth { implicit ctx ⇒
  //   me ⇒ AsyncOk(modLogApi.recent) { html.mod.log(_) }
  // }
}
