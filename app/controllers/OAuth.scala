package controllers

import cats.mtl.Handle.*
import play.api.mvc.*
import scalalib.net.Bearer
import scalatags.Text.all.stringFrag

import lila.app.*
import lila.common.HTTPRequest
import lila.common.Json.given
import lila.oauth.{ AccessToken, AuthorizationRequest, OAuthSignedClient }

import Api.ApiResult

final class OAuth(env: Env, apiC: => Api) extends LilaController(env):

  private def withPrompt(f: (AuthorizationRequest.Prompt, Option[OAuthSignedClient]) => Fu[Result])(using
      ctx: Context
  ): Fu[Result] =
    env.oAuth.signedClients.forReq.fold(
      err => BadRequest.page(views.site.message("Bad authorization request")(stringFrag(err))),
      f.tupled
    )

  def authorize = Open:
    withPrompt: (prompt, signedClient) =>
      val action: OAuthSignedClient.Action = if getBool("signup") then "signup" else "login"
      env.oAuth.signedClients.monitor(signedClient, prompt, action)
      ctx.me match
        case Some(me) =>
          given Me = me
          Ok.page(views.oAuth.authorize(prompt, signedClient))
        case None =>
          Redirect(
            if action == "signup" then routes.Auth.signup.url else routes.Auth.login.url,
            Map("referrer" -> List(req.uri))
          ).toFuccess

  def legacyAuthorize = Anon:
    MovedPermanently(s"${routes.OAuth.authorize}?${req.rawQueryString}")

  def authorizeApply = Auth { _ ?=> me ?=>
    withPrompt: (prompt, _) =>
      allow:
        for
          authorized <- prompt.authorize(me, env.oAuth.legacyClientApi.apply)
          code <- env.oAuth.authorizationApi.create(authorized)
        yield
          lila.mon.user.oauth.authorize("success").increment()
          SeeOther(authorized.redirectUrl(code))
      .rescue: error =>
        lila.oauth.logger.info:
          s"OAuth.authorizeApply ${me.username} error: $error client: ${HTTPRequest.printClient}"
        lila.mon.user.oauth.authorize(error.error).increment()
        SeeOther(prompt.redirectUri.error(error, prompt.state))
  }

  def tokenApply = AnonBodyOf(parse.form(lila.oauth.AccessTokenRequest.form)): raw =>
    for res <- env.oAuth.makeToken(raw, legacy = false)
    yield res.fold(BadRequest(_), Ok(_))

  def legacyTokenApply = AnonBodyOf(parse.form(lila.oauth.AccessTokenRequest.form)): raw =>
    for res <- env.oAuth.makeToken(raw, legacy = true)
    yield res.fold(BadRequest(_), Ok(_))

  def tokenRevoke = Scoped() { ctx ?=> _ ?=>
    HTTPRequest.bearer.so: (bearer, _) =>
      env.oAuth.tokenApi.revoke(bearer).inject(NoContent)
  }

  def revokeClient = AuthBody { ctx ?=> _ ?=>
    bindForm(lila.oauth.AccessTokenRequest.revokeClientForm)(
      _ => BadRequest,
      origin => env.oAuth.tokenApi.revokeByClientOrigin(origin).inject(NoContent)
    )
  }

  def challengeTokens = ScopedBody(_.Web.Mod) { ctx ?=> me ?=>
    if isGranted(_.ApiChallengeAdmin) then
      bindForm(lila.oauth.OAuthTokenForm.adminChallengeTokens())(
        jsonFormError,
        data =>
          for tokens <- env.oAuth.tokenApi.adminChallengeTokens(data, me)
          yield JsonOk(tokens.view.mapValues(_.plain).toMap)
      )
    else Unauthorized(jsonError("Missing permission"))
  }

  def testTokens = AnonBodyOf(parse.tolerantText): body =>
    val bearers = Bearer.from(body.trim.split(',').view.take(1000).toList)
    limit.oauthTokenTest
      .use(fuccess(ApiResult.Limited), cost = bearers.size):
        for tokens <- env.oAuth.tokenApi.test(bearers)
        yield ApiResult.Data(AccessToken.json.test(tokens))
      .map(apiC.toHttp)

  def mobileOAuthCallback = Open:
    Ok.page(lila.web.ui.mobileRedirect)
