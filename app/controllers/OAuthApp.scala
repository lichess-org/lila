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
      setup => {
        val app = setup make me
        env.appApi.create(app) inject Redirect(routes.OAuthApp.edit(app.clientId.value))
      }
    )
  }

  def edit(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(env.appApi.findBy(App.Id(id), me)) { app =>
      Ok(html.oAuth.app.edit(app, env.forms.app.edit(app))).fuccess
    }
  }

  def update(id: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(env.appApi.findBy(App.Id(id), me)) { app =>
      implicit val req = ctx.body
      env.forms.app.edit(app).bindFromRequest.fold(
        err => BadRequest(html.oAuth.app.edit(app, err)).fuccess,
        data => env.appApi.update(app) { data.update(_) } map { r => Redirect(routes.OAuthApp.edit(app.clientId.value)) }
      )
    }
  }

  def delete(id: String) = Auth { implicit ctx => me =>
    env.appApi.deleteBy(App.Id(id), me) inject
      Redirect(routes.OAuthApp.index)
  }
}
