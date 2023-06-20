package controllers

import views.*

import lila.app.{ given, * }
import lila.oauth.{ AccessToken, OAuthTokenForm }

final class OAuthToken(env: Env) extends LilaController(env):

  private val tokenApi = env.oAuth.tokenApi

  def index = Auth { ctx ?=> me ?=>
    Ok.pageAsync:
      tokenApi.listPersonal(me).map(html.oAuth.token.index(_))
  }

  def create = Auth { ctx ?=> me ?=>
    val form = OAuthTokenForm.create fill OAuthTokenForm.Data(
      description = ~get("description"),
      scopes = (~ctx.req.queryString.get("scopes[]")).toList
    )
    Ok.page(html.oAuth.token.create(form, me))
  }

  def createApply = AuthBody { ctx ?=> me ?=>
    OAuthTokenForm.create
      .bindFromRequest()
      .fold(
        err => BadRequest.page(html.oAuth.token.create(err, me)),
        setup =>
          tokenApi.create(setup, me, env.clas.studentCache.isStudent(me)) inject
            Redirect(routes.OAuthToken.index).flashSuccess
      )
  }

  def delete(id: String) = Auth { _ ?=> me ?=>
    tokenApi.revokeById(AccessToken.Id(id), me) inject Redirect(routes.OAuthToken.index).flashSuccess
  }
