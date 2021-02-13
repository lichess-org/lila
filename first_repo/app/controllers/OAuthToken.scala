package controllers

import views._

import lila.app._

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
              Redirect(routes.OAuthToken.index).flashSuccess
        )
    }

  def delete(publicId: String) =
    Auth { _ => me =>
      tokenApi.deleteByPublicId(publicId, me) map {
        _ foreach { token =>
          env.oAuth.server.deleteCached(token.id)
        }
      } inject
        Redirect(routes.OAuthToken.index).flashSuccess
    }
}
