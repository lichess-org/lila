package controllers

import views._

import lila.app._
import lila.oauth.AccessToken

final class OAuthToken(env: Env) extends LilaController(env) {

  private val tokenApi = env.oAuth.tokenApi

  def index =
    Auth { implicit ctx => me =>
      tokenApi.list(me) map { tokens =>
        Ok(html.oAuth.token.index(tokens))
      }
    }

  def create =
    Auth { implicit ctx => me =>
      val form = env.oAuth.forms.token.create fill lila.oauth.OAuthForm.token
        .Data(
          description = ~get("description"),
          scopes = (~ctx.req.queryString.get("scopes[]")).toList
        )
      Ok(html.oAuth.token.create(form, me)).fuccess
    }

  def createApply =
    AuthBody { implicit ctx => me =>
      implicit val req = ctx.body
      env.oAuth.forms.token.create
        .bindFromRequest()
        .fold(
          err => BadRequest(html.oAuth.token.create(err, me)).fuccess,
          setup =>
            tokenApi.create(setup make me) inject
              Redirect(routes.OAuthToken.index()).flashSuccess
        )
    }

  def delete(id: String) =
    Auth { _ => me =>
      val tokenId = AccessToken.Id(id)
      tokenApi.deleteBy(tokenId, me) >>-
        env.oAuth.server.deleteCached(tokenId) inject
        Redirect(routes.OAuthToken.index()).flashSuccess
    }
}
