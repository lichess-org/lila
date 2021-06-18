package controllers

import lila.app._
import lila.oauth.{ AccessToken, OAuthApp => App }
import views._

final class OAuthApp(env: Env) extends LilaController(env) {

  private val appApi = env.oAuth.appApi
  private val forms  = env.oAuth.forms

  def index =
    Auth { implicit ctx => me =>
      appApi.mine(me) flatMap { made =>
        appApi.authorizedBy(me) map { used =>
          Ok(html.oAuth.app.index(made, used))
        }
      }
    }

  def delete(id: String) =
    Auth { _ => me =>
      appApi.deleteBy(App.Id(id), me) inject
        Redirect(s"${routes.OAuthApp.index}#made").flashSuccess
    }

  def revoke(id: String) =
    Auth { _ => me =>
      appApi.revoke(AccessToken.Id(id), me) inject
        Redirect(routes.OAuthApp.index).flashSuccess
    }
}
