package controllers

import lila.app._
import lila.oauth.AccessToken
import views._

final class OAuthToken(env: Env) extends LilaController(env) {

  private val tokenApi = env.oAuth.tokenApi

  def index = Auth { implicit ctx => me =>
    tokenApi.list(me) map { tokens =>
      Ok(html.oAuth.token.index(tokens))
    }
  }

  def create = Auth { implicit ctx => me =>
    Ok(html.oAuth.token.create(env.oAuth.forms.token.create, me)).fuccess
  }

  def createApply = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    env.oAuth.forms.token.create.bindFromRequest.fold(
      err => BadRequest(html.oAuth.token.create(err, me)).fuccess,
      setup =>
        tokenApi.create(setup make me) inject
          Redirect(routes.OAuthToken.index)
    )
  }

  def delete(id: String) = Auth { _ => me =>
    tokenApi.deleteBy(AccessToken.Id(id), me) inject
      Redirect(routes.OAuthToken.index)
  }
}
