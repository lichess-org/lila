package controllers

import lila.app.{ *, given }
import lila.oauth.{ AccessToken, OAuthTokenForm }

final class OAuthToken(env: Env) extends LilaController(env):

  private val tokenApi = env.oAuth.tokenApi

  def index = Auth { ctx ?=> me ?=>
    Ok.async:
      tokenApi.listPersonal.map(views.oAuth.token.index(_))
  }

  def create = Auth { ctx ?=> me ?=>
    val form = OAuthTokenForm.create.fill(
      OAuthTokenForm.Data(
        description = ~get("description"),
        scopes = (~ctx.req.queryString.get("scopes[]")).toList
      )
    )
    Ok.page(views.oAuth.token.create(form, me))
  }

  def createApply = AuthBody { ctx ?=> me ?=>
    bindForm(OAuthTokenForm.create)(
      err => BadRequest.page(views.oAuth.token.create(err, me)),
      setup =>
        tokenApi
          .create(setup, env.clas.studentCache.isStudent(me))
          .inject(Redirect(routes.OAuthToken.index).flashSuccess)
    )
  }

  def delete(id: String) = Auth { _ ?=> _ ?=>
    tokenApi.revokeById(AccessToken.Id(id)).inject(Redirect(routes.OAuthToken.index).flashSuccess)
  }
