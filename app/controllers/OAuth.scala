package controllers

import play.api.libs.json.{ JsNull, JsObject, JsValue, Json }
import play.api.mvc.*
import scalalib.ThreadLocalRandom
import scalatags.Text.all.stringFrag

import lila.app.*
import lila.common.HTTPRequest
import lila.common.Json.given
import lila.core.net.Bearer
import lila.oauth.{ AccessTokenRequest, AuthorizationRequest, OAuthScopes }

import Api.ApiResult

final class OAuth(env: Env, apiC: => Api) extends LilaController(env):

  private def reqToAuthorizationRequest(using RequestHeader) =
    import lila.oauth.Protocol.*
    AuthorizationRequest.Raw(
      clientId = getAs[ClientId]("client_id"),
      responseType = get("response_type"),
      redirectUri = get("redirect_uri"),
      state = getAs[State]("state"),
      codeChallengeMethod = get("code_challenge_method"),
      codeChallenge = getAs[CodeChallenge]("code_challenge"),
      scope = get("scope"),
      username = getAs[UserStr]("username")
    )

  private def withPrompt(f: AuthorizationRequest.Prompt => Fu[Result])(using ctx: Context): Fu[Result] =
    reqToAuthorizationRequest.prompt match
      case Right(prompt) =>
        AuthorizationRequest.logPrompt(prompt, ctx.me)
        f(prompt)
      case Left(error) =>
        BadRequest.page(views.site.message("Bad authorization request")(stringFrag(error.description)))

  def authorize = Open:
    withPrompt: prompt =>
      ctx.me.fold(Redirect(routes.Auth.login.url, Map("referrer" -> List(req.uri))).toFuccess): me =>
        Ok.page:
          views.oAuth.authorize(prompt, me, s"${routes.OAuth.authorizeApply}?${req.rawQueryString}")

  def legacyAuthorize = Anon:
    MovedPermanently(s"${routes.OAuth.authorize}?${req.rawQueryString}")

  def authorizeApply = Auth { _ ?=> me ?=>
    withPrompt: prompt =>
      prompt
        .authorize(me, env.oAuth.legacyClientApi.apply)
        .flatMap:
          case Right(authorized) =>
            env.oAuth.authorizationApi.create(authorized).map { code =>
              SeeOther(authorized.redirectUrl(code))
            }
          case Left(error) => SeeOther(prompt.redirectUri.error(error, prompt.state))
  }

  def tokenApply = AnonBodyOf(parse.form(lila.oauth.AccessTokenRequest.form)):
    _.prepare match
      case Right(prepared) =>
        env.oAuth.authorizationApi
          .consume(prepared)
          .flatMap:
            case Right(granted) =>
              env.oAuth.tokenApi.create(granted).map { token =>
                Ok(
                  Json
                    .obj(
                      "token_type"   -> "Bearer",
                      "access_token" -> token.plain
                    )
                    .add("expires_in" -> token.expires.map(_.toSeconds - nowSeconds))
                )
              }
            case Left(err) => BadRequest(err.toJson)
      case Left(err) => BadRequest(err.toJson)

  def legacyTokenApply = AnonBodyOf(parse.form(lila.oauth.AccessTokenRequest.form)):
    _.prepareLegacy(AccessTokenRequest.BasicAuth.from(req)) match
      case Right(prepared) =>
        env.oAuth.authorizationApi
          .consume(prepared)
          .flatMap:
            case Right(granted) =>
              env.oAuth.tokenApi.create(granted).map { token =>
                Ok(
                  Json
                    .obj(
                      "token_type"    -> "Bearer",
                      "access_token"  -> token.plain,
                      "refresh_token" -> s"invalid_for_bc_${ThreadLocalRandom.nextString(17)}"
                    )
                    .add("expires_in" -> token.expires.map(_.toSeconds - nowSeconds))
                )
              }
            case Left(err) => BadRequest(err.toJson)
      case Left(err) => BadRequest(err.toJson)

  def tokenRevoke = Scoped() { ctx ?=> _ ?=>
    HTTPRequest.bearer(ctx.req).so { token =>
      env.oAuth.tokenApi.revoke(token).inject(NoContent)
    }
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
          env.oAuth.tokenApi
            .adminChallengeTokens(data, me)
            .map: tokens =>
              JsonOk(tokens.view.mapValues(_.plain).toMap)
      )
    else Unauthorized(jsonError("Missing permission"))
  }

  def testTokens = AnonBodyOf(parse.tolerantText): body =>
    val bearers = Bearer.from(body.trim.split(',').view.take(1000).toList)
    limit
      .oauthTokenTest(req.ipAddress, fuccess(ApiResult.Limited), cost = bearers.size):
        env.oAuth.tokenApi.test(bearers).map { tokens =>
          import lila.common.Json.given
          ApiResult.Data(JsObject(tokens.map { (bearer, token) =>
            bearer.value -> token.fold[JsValue](JsNull): t =>
              Json.obj(
                "userId"  -> t.userId,
                "scopes"  -> t.scopes.into(OAuthScopes).keyList,
                "expires" -> t.expires
              )
          }))
        }
      .map(apiC.toHttp)
