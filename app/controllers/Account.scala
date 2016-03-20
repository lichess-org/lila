package controllers

import play.api.mvc._, Results._

import lila.api.Context
import lila.app._
import lila.common.LilaCookie
import lila.db.api.$find
import lila.security.Permission
import lila.user.tube.userTube
import lila.user.{ User => UserModel, UserRepo }
import views._

object Account extends LilaController {

  private def env = Env.user
  private def relationEnv = Env.relation
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

  def info = Open { implicit ctx =>
    negotiate(
      html = notFound,
      api = _ => ctx.me match {
        case None => fuccess(unauthorizedApiResult)
        case Some(me) =>
          relationEnv.api.countFollowers(me.id) zip
            relationEnv.api.countFollowing(me.id) zip
            Env.pref.api.getPref(me) zip
            lila.game.GameRepo.urgentGames(me) map {
              case (((nbFollowers, nbFollowing), prefs), povs) =>
                Env.current.bus.publish(lila.user.User.Active(me), 'userActive)
                Ok {
                  import play.api.libs.json._
                  import lila.pref.JsonView._
                  Env.user.jsonView(me) ++ Json.obj(
                    "prefs" -> prefs,
                    "nowPlaying" -> JsArray(povs take 20 map Env.api.lobbyApi.nowPlaying),
                    "nbFollowing" -> nbFollowing,
                    "nbFollowers" -> nbFollowers)
                }
            }
      }
    ) map ensureSessionId(ctx.req)
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
          ok ← UserRepo.checkPasswordById(me.id, data.oldPasswd)
          _ ← ok ?? UserRepo.passwd(me.id, data.newPasswd1)
        } yield {
          val content = html.account.passwd(me, forms.passwd.fill(data), ok.some)
          ok.fold(Ok(content), BadRequest(content))
        }
      }
  }

  private def emailForm(user: UserModel) = UserRepo email user.id map { email =>
    Env.security.forms.changeEmail(user).fill(
      lila.security.DataForm.ChangeEmail(~email, ""))
  }

  def email = Auth { implicit ctx =>
    me =>
      emailForm(me) map { form =>
        Ok(html.account.email(me, form))
      }
  }

  def emailApply = AuthBody { implicit ctx =>
    me =>
      UserRepo hasEmail me.id flatMap {
        case true => notFound
        case false =>
          implicit val req = ctx.body
          FormFuResult(Env.security.forms.changeEmail(me)) { err =>
            fuccess(html.account.email(me, err))
          } { data =>
            val email = Env.security.emailAddress.validate(data.email) err s"Invalid email ${data.email}"
            for {
              ok ← UserRepo.checkPasswordById(me.id, data.passwd)
              _ ← ok ?? UserRepo.email(me.id, email)
              form <- emailForm(me)
            } yield {
              val content = html.account.email(me, form, ok.some)
              ok.fold(Ok(content), BadRequest(content))
            }
          }
      }
  }

  def close = Auth { implicit ctx =>
    me =>
      Ok(html.account.close(me, Env.security.forms.closeAccount)).fuccess
  }

  def closeConfirm = AuthBody { implicit ctx =>
    me =>
      implicit val req = ctx.body
      FormFuResult(Env.security.forms.closeAccount) { err =>
        fuccess(html.account.close(me, err))
      } { password =>
        UserRepo.checkPasswordById(me.id, password) flatMap {
          case false => BadRequest(html.account.close(me, Env.security.forms.closeAccount)).fuccess
          case true => doClose(me) inject {
            Redirect(routes.User show me.username) withCookies LilaCookie.newSession
          }
        }
      }
  }

  private[controllers] def doClose(user: UserModel) =
    (UserRepo disable user) >>-
      env.onlineUserIdMemo.remove(user.id) >>
      relationEnv.api.unfollowAll(user.id) >>
      Env.team.api.quitAll(user.id) >>-
      Env.challenge.api.removeByUserId(user.id) >>-
      Env.tournament.api.withdrawAll(user) >>
      (Env.security disconnect user.id)

  def kid = Auth { implicit ctx =>
    me =>
      Ok(html.account.kid(me)).fuccess
  }

  def kidConfirm = Auth { ctx =>
    me =>
      implicit val req = ctx.req
      (UserRepo toggleKid me) inject Redirect(routes.Account.kid)
  }

  private def currentSessionId(implicit ctx: Context) =
    ~Env.security.api.reqSessionId(ctx.req)

  def security = Auth { implicit ctx =>
    me =>
      Env.security.api.dedup(me.id, ctx.req) >>
        Env.security.api.locatedOpenSessions(me.id, 50) map { sessions =>
          Ok(html.account.security(me, sessions, currentSessionId))
        }
  }

  def signout(sessionId: String) = Auth { implicit ctx =>
    me =>
      if (sessionId == "all")
        lila.security.Store.closeUserExceptSessionId(me.id, currentSessionId) inject
          Redirect(routes.Account.security)
      else
        lila.security.Store.closeUserAndSessionId(me.id, sessionId)
  }
}
