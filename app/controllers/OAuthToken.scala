package controllers

import lila.app.{ *, given }
import lila.core.misc.oauth.AccessTokenId
import lila.oauth.OAuthTokenForm

final class OAuthToken(env: Env) extends LilaController(env):

  private val tokenApi = env.oAuth.tokenApi

  def index = Auth { ctx ?=> me ?=>
    for
      tokens <- tokenApi.listPersonal
      page <- Ok.page(views.oAuth.token.index(tokens))
    yield page.hasPersonalData
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
        for _ <- tokenApi.create(setup, env.clas.studentCache.isStudent(me))
        yield Redirect(routes.OAuthToken.index).flashSuccess
    )
  }

  def delete(id: String) = Auth { _ ?=> _ ?=>
    tokenApi.revokeById(AccessTokenId(id)).inject(Redirect(routes.OAuthToken.index).flashSuccess)
  }
