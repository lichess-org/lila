package controllers

import lila.api.Context
import lila.app._
import lila.common.{ LilaCookie, HTTPRequest }
import lila.user.{ UserRepo, User => UserModel }
import views._

import play.api.mvc._

object SoclogOAuth2 extends LilaController {

  def start(providerName: String) = Open { implicit ctx =>
    implicit val req = ctx.req
    // WithProvider(providerName)(api.start)
    ???
  }

  private def WithProvider(name: String)(f: lila.soclog.oauth2.OAuth2Provider => Fu[Result])(implicit ctx: Context) =
    Env.soclog.oauth2.providers(name).fold(notFound)(f)

  // private def NewUser(oAuthId: String)(f: lila.soclog.oauth2.OAuth2 => Fu[Result])(implicit ctx: Context) =
  //   OptionFuResult(api findOAuth oAuthId) { oAuth =>
  //     UserRepo bySoclog oAuth.id flatMap {
  //       case None       => f(oAuth)
  //       case Some(user) => Redirect(routes.User.show(user.username)).fuccess
  //     }
  //   }
}
