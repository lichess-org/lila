package controllers

import play.api.libs.json.JsValue
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.oauth.{ OAuthApp => App }
import views._

object OAuthApp extends LilaController {

  private val env = Env.oAuth

  def index = Auth { implicit ctx => me =>
    env.appApi.list(me) map { apps =>
      Ok(html.oAuth.app.index(apps))
    }
  }

  def create = Auth { implicit ctx => me =>
    Ok(html.oAuth.app.create(env.forms.app.create)).fuccess
  }

  def createApply = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    env.forms.app.create.bindFromRequest.fold(
      err => BadRequest(html.oAuth.app.create(err)).fuccess,
      setup => env.appApi.create(setup make me) inject
        Redirect(routes.OAuthApp.index)
    )
  }

  def delete(id: String) = Auth { implicit ctx => me =>
    env.appApi.deleteBy(App.Id(id), me) inject
      Redirect(routes.OAuthApp.index)
  }
}
