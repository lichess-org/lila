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

  def info = Auth { implicit ctx =>
    me =>
      negotiate(
        html = notFound,
        api = _ => lila.game.GameRepo nowPlaying me map { povs =>
          Ok {
            import play.api.libs.json._
            Env.user.jsonView(me, extended = true) ++ Json.obj(
              "nowPlaying" -> JsArray(povs map { pov =>
                Json.obj(
                  "id" -> pov.fullId,
                  "variant" -> pov.game.variant.key,
                  "speed" -> pov.game.speed.key,
                  "perf" -> lila.game.PerfPicker.key(pov.game),
                  "rated" -> pov.game.rated,
                  "opponent" -> Json.obj(
                    "id" -> pov.opponent.userId,
                    "username" -> lila.game.Namer.playerString(pov.opponent, withRating = false)(Env.user.lightUser),
                    "rating" -> pov.opponent.rating),
                  "secondsLeft" -> pov.game.correspondenceClock.map(_ remainingTime pov.color toInt))
              })
            )
          }
        }
      )
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
