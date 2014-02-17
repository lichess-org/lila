package controllers

import play.api.mvc._, Results._

import lila.app._
import lila.common.LilaCookie
import lila.db.api.$find
import lila.security.Permission
import lila.user.tube.userTube
import lila.user.{ User => UserModel, UserRepo }
import views._

object Account extends LilaController {

  private def env = Env.user
  private def forms = lila.user.DataForm

  def profile = Auth { implicit ctx =>
    me =>
      Ok(html.account.profile(me, forms profileOf me)).fuccess
  }

  def profileApply = AuthBody { implicit ctx =>
    me =>
      implicit val req: Request[_] = ctx.body
      FormFuResult(forms.profile) { err =>
        fuccess(html.account.profile(me, err))
      } { profile =>
        UserRepo.setProfile(me.id, profile) inject Redirect(routes.User show me.username)
      }
  }

  def passwd = Auth { implicit ctx =>
    me =>
      Ok(html.account.passwd(me, forms.passwd)).fuccess
  }

  def passwdApply = AuthBody { implicit ctx =>
    me =>
      implicit val req = ctx.body
      FormFuResult(forms.passwd) { err =>
        fuccess(html.account.passwd(me, err))
      } { passwd =>
        for {
          ok ← UserRepo.checkPassword(me.id, passwd.oldPasswd)
          _ ← ok ?? UserRepo.passwd(me.id, passwd.newPasswd1)
        } yield ok.fold(
          Redirect(routes.User show me.username),
          BadRequest(html.account.passwd(me, forms.passwd))
        )
      }
  }

  def close = Auth { implicit ctx =>
    me =>
      Ok(html.account.close(me)).fuccess
  }

  def closeConfirm = Auth { ctx =>
    me =>
      implicit val req = ctx.req
      (UserRepo disable me.id) >>
        Env.team.api.quitAll(me.id) >>
        (Env.security disconnect me.id) inject {
          Redirect(routes.User show me.username) withCookies LilaCookie.newSession
        }
  }
}
