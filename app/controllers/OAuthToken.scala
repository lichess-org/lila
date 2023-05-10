package controllers

import views.*

import lila.app.{ given, * }
import lila.oauth.{ AccessToken, OAuthTokenForm }

final class OAuthToken(env: Env) extends LilaController(env):

  private val tokenApi = env.oAuth.tokenApi

  def index = Auth { ctx ?=> me =>
    tokenApi.listPersonal(me) map { tokens =>
      Ok(html.oAuth.token.index(tokens))
    }
  }

  def create = Auth { ctx ?=> me =>
    val form = OAuthTokenForm.create fill OAuthTokenForm.Data(
      description = ~get("description"),
      scopes = (~ctx.req.queryString.get("scopes[]")).toList
    )
    Ok(html.oAuth.token.create(form, me)).toFuccess
  }

  def createApply = AuthBody { ctx ?=> me =>
    OAuthTokenForm.create
      .bindFromRequest()
      .fold(
        err => BadRequest(html.oAuth.token.create(err, me)).toFuccess,
        setup =>
          tokenApi.create(setup, me, env.clas.studentCache.isStudent(me.id)) inject
            Redirect(routes.OAuthToken.index).flashSuccess
      )
  }

  def delete(id: String) = Auth { _ ?=> me =>
    tokenApi.revokeById(AccessToken.Id(id), me) inject Redirect(routes.OAuthToken.index).flashSuccess
  }
