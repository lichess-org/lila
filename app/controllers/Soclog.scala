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
  import lila.soclog.SignUpResult._

  def start(providerName: String) = Open { implicit ctx =>
    implicit val req = ctx.req
    WithProvider(providerName)(api.start)
  }

  def finish(providerName: String) = Open { implicit ctx =>
    implicit val req = ctx.req
    WithProvider(providerName) { provider =>
      api.finish(provider) flatMap {
        case Authenticated(user)               => authenticateUser(user)
        case SignedUp(user)                    => authenticateUser(user)
        case ExistingUsername(oauth, existing) => ???
        case x                                 => ???
      }
    }
  }

  private def authenticateUser(u: UserModel)(implicit ctx: Context): Fu[Result] = {
    implicit val req = ctx.req
    if (u.ipBan) fuccess(Redirect(routes.Lobby.home))
    else security.saveAuthentication(u.id, ctx.mobileApiVersion) map { sessionId =>
      Redirect {
        get("referrer").filter(_.nonEmpty) orElse req.session.get(security.AccessUri) getOrElse routes.Lobby.home.url
      } withCookies LilaCookie.withSession { session =>
        session + ("sessionId" -> sessionId) - security.AccessUri
      }
    }
  }

  private def WithProvider(name: String)(f: lila.soclog.Provider => Fu[Result])(implicit ctx: Context) =
    Env.soclog.providers(name).fold(notFound)(f)
}
