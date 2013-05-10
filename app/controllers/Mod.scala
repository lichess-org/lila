package controllers

import lila.app._
import views._
import lila.security.Permission

import play.api.mvc._
import play.api.mvc.Results._

object Mod extends LilaController {

  private def modApi = Env.mod.api
  private def modLogApi = Env.mod.logApi

  def engine(username: String) = Secure(_.MarkEngine) { _ ⇒
    me ⇒ modApi.adjust(me.id, username) inject Redirect(routes.User show username)
  }

  def mute(username: String) = Secure(_.MutePlayer) { _ ⇒
    me ⇒ modApi.mute(me.id, username) inject Redirect(routes.User show username)
  }

  def ban(username: String) = Secure(_.IpBan) { implicit ctx ⇒
    me ⇒ modApi.ban(me.id, username) inject Redirect(routes.User show username)
  }

  def ipban(ip: String) = Secure(_.IpBan) { implicit ctx ⇒
    me ⇒ modApi.ipban(me.id, ip)
  }

  def log = Auth { implicit ctx ⇒
    me ⇒ modLogApi.recent map { html.mod.log(_) }
  }
}
