package controllers

import lila._
import views._
import security.Permission
import http.Context

import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.concurrent.Execution.Implicits._
import scalaz.effects._

object Mod extends LilaController {

  private def modApi = env.mod.api
  private def modLogApi = env.mod.logApi

  def engine(username: String) = Secure(Permission.MarkEngine) { _ ⇒
    me ⇒ AsyncRedirect {
      modApi.adjust(me, username) map { _ ⇒ routes.User show username }
    }
  }

  def mute(username: String) = Secure(Permission.MutePlayer) { _ ⇒
    me ⇒ AsyncRedirect {
      modApi.mute(me, username) map { _ ⇒ routes.User show username }
    }
  }

  def ban(username: String) = Secure(Permission.IpBan) { implicit ctx ⇒
    me ⇒ AsyncRedirect {
      modApi.ban(me, username) map { _ ⇒ routes.User show username }
    }
  }

  def ipban(ip: String) = Secure(Permission.IpBan) { implicit ctx ⇒
    me ⇒ AsyncOk(modApi.ipban(me, ip))
  }

  val log = Auth { implicit ctx ⇒
    me ⇒ Async {
      modLogApi.recent map { docs ⇒
        Ok(html.mod.log(docs))
      }
    }
  }
}
