package controllers

import play.api.libs.json.JsValue
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.oauth.AccessToken
import views._

object OAuthToken extends LilaController {

  private val env = Env.oAuth

  def index = Auth { implicit ctx => me =>
    env.tokenApi.list(me) map { tokens =>
      Ok(html.oAuth.token.index(tokens))
    }
  }

  def create = Auth { implicit ctx => me =>
    lila.user.UserRepo.isBot(me) map { isBot =>
      Ok(html.oAuth.token.create(env.forms.token.create(isBot)))
    }
  }

  def createApply = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    lila.user.UserRepo.isBot(me) flatMap { isBot =>
      env.forms.token.create(isBot).bindFromRequest.fold(
        err => BadRequest(html.oAuth.token.create(err)).fuccess,
        setup => env.tokenApi.create(setup make me) inject
          Redirect(routes.OAuthToken.index)
      )
    }
  }

  def delete(id: String) = Auth { implicit ctx => me =>
    env.tokenApi.deleteBy(AccessToken.Id(id), me) inject
      Redirect(routes.OAuthToken.index)
  }
}
