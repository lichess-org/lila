package controllers

import lila._
import views._
import security.Permission
import http.Context

import play.api.mvc._
import play.api.mvc.Results._
import scalaz.effects._

object Mod extends LilaController {

  def modApi = env.mod.api
  def modLogApi = env.mod.logApi

  def engine(username: String) = Secure(Permission.MarkEngine) { _ ⇒
    me ⇒
      IORedirect {
        modApi.adjust(me, username) map { _ ⇒ routes.User show username }
      }
  }

  def mute(username: String) = Secure(Permission.MutePlayer) { _ ⇒
    me ⇒
      IORedirect {
        modApi.mute(me, username) map { _ ⇒ routes.User show username }
      }
  }

  def ban(username: String) = Secure(Permission.IpBan) { implicit ctx ⇒
    me ⇒
      IORedirect {
        modApi.ban(me, username) map { _ ⇒ routes.User show username }
      }
  }

  def ipban(ip: String) = Secure(Permission.IpBan) { implicit ctx ⇒
    me ⇒
      IOk(modApi.ipban(me, ip))
  }

  val log = Auth { implicit ctx ⇒
    me =>
      IOk(modLogApi.recent map { html.mod.log(_) })
  }
}
