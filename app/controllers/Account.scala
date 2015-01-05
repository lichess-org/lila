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

  def info = Auth { implicit ctx =>
    me =>
      negotiate(
        html = notFound,
        api = _ => lila.game.GameRepo nowPlaying me map { povs =>
          Ok {
            import play.api.libs.json._
            Env.user.jsonView(me, extended = true) ++ Json.obj(
              "nowPlaying" -> JsArray(povs map Env.api.lobbyApi.nowPlaying))
          }
        }
      )
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
      } { data =>
        for {
          ok ← UserRepo.checkPassword(me.id, data.oldPasswd)
          _ ← ok ?? UserRepo.passwd(me.id, data.newPasswd1)
        } yield {
          val content = html.account.passwd(me, forms.passwd.fill(data), ok.some)
          ok.fold(Ok(content), BadRequest(content))
        }
      }
  }

  private def emailForm(id: String) = UserRepo email id map { email =>
    forms.email.fill(forms.Email(~email, ""))
  }

  def email = Auth { implicit ctx =>
    me =>
      emailForm(me.id) map { form =>
        Ok(html.account.email(me, form))
      }
  }

  def emailApply = AuthBody { implicit ctx =>
    me =>
      implicit val req = ctx.body
      FormFuResult(forms.email) { err =>
        fuccess(html.account.email(me, err))
      } { data =>
        for {
          ok ← UserRepo.checkPassword(me.id, data.passwd)
          _ ← ok ?? UserRepo.email(me.id, data.email)
          form <- emailForm(me.id)
        } yield {
          val content = html.account.email(me, form, ok.some)
          ok.fold(Ok(content), BadRequest(content))
        }
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
