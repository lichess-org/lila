package controllers

import lila.api.Context
import lila.app._
import lila.common.{ LilaCookie, HTTPRequest }
import lila.user.{ UserRepo, User => UserModel }
import views._

import play.api.mvc._

object Soclog extends LilaController {

  val api = Env.soclog.api
  val security = Env.security.api

  import lila.soclog.AuthResult._

  def start(providerName: String) = Open { implicit ctx =>
    implicit val req = ctx.req
    WithProvider(providerName)(api.start)
  }

  def finish(providerName: String) = Open { implicit ctx =>
    implicit val req = ctx.req
    WithProvider(providerName) { provider =>
      api.finish(provider) flatMap {
        case Authenticated(user) => authenticateUser(user)
        case PickUsername(oauth) => Redirect(routes.Soclog.username(oauth.id)).fuccess
        case UtterFail => BadRequest(html.soclog.fail {
          "Sorry, we cannot log you in!"
        }).fuccess
        case AccessDenied => Ok(html.soclog.fail {
          "The access was denied, you are not logged in."
        }).fuccess
      }
    }
  }

  def username(oAuthId: String) = Open { implicit ctx =>
    NewUser(oAuthId) { oAuth =>
      Ok(html.soclog.username(oAuth, Env.security.forms.signup.soclog.fill(oAuth.profile.username))).fuccess
    }
  }

  def setUsername(oAuthId: String) = OpenBody { implicit ctx =>
    NewUser(oAuthId) { oAuth =>
      implicit val req = ctx.body
      Env.security.forms.signup.soclog.bindFromRequest.fold(
        err => BadRequest(html.soclog.username(oAuth, err)).fuccess,
        username => UserRepo.createSoclog(oAuth.id, username).flatMap {
          case None       => Redirect(routes.Soclog.username(oAuth.id)).fuccess
          case Some(user) => authenticateUser(user, routes.User.show(user.username).some)
        })
    }
  }

  private def authenticateUser(u: UserModel, redirectTo: Option[Call] = none)(implicit ctx: Context): Fu[Result] = {
    implicit val req = ctx.req
    if (u.ipBan) fuccess(Redirect(routes.Lobby.home))
    else security.saveAuthentication(u.id, ctx.mobileApiVersion) map { sessionId =>
      Redirect {
        redirectTo.map(_.url) orElse
          get("referrer").filter(_.nonEmpty) orElse
          req.session.get(security.AccessUri) getOrElse
          routes.Lobby.home.url
      } withCookies LilaCookie.withSession { session =>
        session + ("sessionId" -> sessionId) - security.AccessUri - lila.soclog.SoclogApi.cacheKey
      }
    }
  }

  private def WithProvider(name: String)(f: lila.soclog.Provider => Fu[Result])(implicit ctx: Context) =
    Env.soclog.providers(name).fold(notFound)(f)

  private def NewUser(oAuthId: String)(f: lila.soclog.OAuth => Fu[Result])(implicit ctx: Context) =
    OptionFuResult(api findOAuth oAuthId) { oAuth =>
      UserRepo bySoclog oAuth.id flatMap {
        case None       => f(oAuth)
        case Some(user) => Redirect(routes.User.show(user.username)).fuccess
      }
    }
}
