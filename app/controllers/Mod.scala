package controllers

import lila.app._
import lila.security.Permission
import lila.user.UserRepo
import views._

import org.joda.time.DateTime
import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json.Json

import lila.evaluation.{ PlayerAssessment }

import chess.Color

object Mod extends LilaController {

  private def modApi = Env.mod.api
  private def modLogApi = Env.mod.logApi
  private def assessApi = Env.mod.assessApi

  def engine(username: String) = Secure(_.MarkEngine) { _ =>
    me => modApi.toggleEngine(me.id, username) inject redirect(username)
  }

  def booster(username: String) = Secure(_.MarkBooster) { _ =>
    me => modApi.toggleBooster(me.id, username) inject redirect(username)
  }

  def troll(username: String) = Secure(_.MarkTroll) { implicit ctx =>
    me =>
      modApi.troll(me.id, username, getBool("set")) inject {
        get("then") match {
          case Some("reports") => Redirect(routes.Report.list)
          case _               => redirect(username)
        }
      }
  }

  def ban(username: String) = Secure(_.IpBan) { implicit ctx =>
    me => modApi.ban(me.id, username) inject redirect(username)
  }

  def ipban(ip: String) = Secure(_.IpBan) { implicit ctx =>
    me => modApi.ipban(me.id, ip)
  }

  def closeAccount(username: String) = Secure(_.CloseAccount) { implicit ctx =>
    me => modApi.closeAccount(me.id, username) >>
      Env.relation.api.unfollowAll(lila.user.User normalize username) inject redirect(username)
  }

  def reopenAccount(username: String) = Secure(_.ReopenAccount) { implicit ctx =>
    me => modApi.reopenAccount(me.id, username) inject redirect(username)
  }

  def setTitle(username: String) = AuthBody { implicit ctx =>
    me =>
      implicit def req = ctx.body
      if (isGranted(_.SetTitle))
        lila.user.DataForm.title.bindFromRequest.fold(
          err => fuccess(redirect(username, mod = true)),
          title => modApi.setTitle(me.id, username, title) inject redirect(username, mod = false)
        )
      else fuccess(authorizationFailed(ctx.req))
  }

  def setEmail(username: String) = AuthBody { implicit ctx =>
    me =>
      implicit def req = ctx.body
      OptionFuResult(UserRepo named username) { user =>
        if (isGranted(_.SetEmail) && !isGranted(_.SetEmail, user))
          Env.security.forms.modEmail(user).bindFromRequest.fold(
            err => BadRequest(err.toString).fuccess,
            email => modApi.setEmail(me.id, user.id, email) inject redirect(user.username, mod = true)
          )
        else fuccess(authorizationFailed(ctx.req))
      }
  }

  def log = Secure(_.SeeReport) { implicit ctx =>
    me => modLogApi.recent map { html.mod.log(_) }
  }

  def communication(username: String) = Secure(_.MarkTroll) { implicit ctx =>
    me =>
      OptionFuOk(UserRepo named username) { user =>
        for {
          povs <- lila.game.GameRepo.recentPovsByUser(user, 100)
          chats <- povs.map(p => Env.chat.api.playerChat findNonEmpty p.gameId).sequence
          povWithChats = (povs zip chats) collect {
            case (p, Some(c)) => p -> c
          } take 9
          threads <- {
            lila.message.ThreadRepo.visibleByUser(user.id, 50) map {
              _ filter (_ hasPostsWrittenBy user.id) take 9
            }
          }
          publicLines <- Env.shutup.api getPublicLines user.id
          spy <- Env.security userSpy user.id
        } yield html.mod.communication(user, povWithChats, threads, publicLines, spy)
      }
  }

  private val ipIntelCache =
    lila.memo.AsyncCache[String, String](ip => {
      import play.api.libs.ws.WS
      import play.api.Play.current
      val email = "lichess.contact@gmail.com"
      val url = s"http://check.getipintel.net/check.php?ip=$ip&contact=$email"
      WS.url(url).get().map(_.body)
    }, maxCapacity = 2000)

  def ipIntel(ip: String) = Secure(_.IpBan) { ctx =>
    me =>
      ipIntelCache(ip).map { Ok(_) }
  }

  def whois(ip: String) = Open { implicit ctx =>
    Env.security.whois(ip, ~get("key")) map {
      case Left(msg)   => BadRequest(Json.obj("error" -> msg))
      case Right(data) => Ok(data)
    }
  }

  def redirect(username: String, mod: Boolean = true) = Redirect(routes.User.show(username).url + mod.??("?mod"))

  def refreshUserAssess(username: String) = Secure(_.MarkEngine) { implicit ctx =>
    me => assessApi.refreshAssessByUsername(username) inject redirect(username)
  }
}
