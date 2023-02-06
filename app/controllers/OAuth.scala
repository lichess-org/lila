package controllers

import cats.data.Validated
import play.api.data.Form
import play.api.data.Forms.*
import play.api.libs.json.{ JsNull, JsObject, JsString, JsValue, Json }
import play.api.mvc.*
import scalatags.Text.all.stringFrag
import views.*
import ornicar.scalalib.ThreadLocalRandom

import lila.api.Context
import lila.app.{ given, * }
import lila.common.{ HTTPRequest, IpAddress, Bearer }
import lila.common.Json.given
import lila.oauth.{ AccessToken, AccessTokenRequest, AuthorizationRequest }
import Api.ApiResult

final class OAuth(env: Env, apiC: => Api) extends LilaController(env):

  private def reqToAuthorizationRequest(req: RequestHeader) =
    import lila.oauth.Protocol.*
    AuthorizationRequest.Raw(
      clientId = ClientId from get("client_id", req),
      responseType = get("response_type", req),
      redirectUri = get("redirect_uri", req),
      state = State from get("state", req),
      codeChallengeMethod = get("code_challenge_method", req),
      codeChallenge = CodeChallenge from get("code_challenge", req),
      scope = get("scope", req),
      username = UserStr from get("username", req)
    )

  private def withPrompt(f: AuthorizationRequest.Prompt => Fu[Result])(using ctx: Context) =
    reqToAuthorizationRequest(ctx.req).prompt match
      case Validated.Valid(prompt) => f(prompt)
      case Validated.Invalid(error) =>
        BadRequest(html.site.message("Bad authorization request")(stringFrag(error.description))).toFuccess

  def authorize =
    Open { implicit ctx =>
      withPrompt { prompt =>
        fuccess(ctx.me.fold(Redirect(routes.Auth.login.url, Map("referrer" -> List(ctx.req.uri)))) { me =>
          Ok {
            html.oAuth.authorize(prompt, me, s"${routes.OAuth.authorizeApply}?${ctx.req.rawQueryString}")
          }
        })
      }
    }

  def legacyAuthorize =
    Action { (req: RequestHeader) =>
      MovedPermanently(s"${routes.OAuth.authorize}?${req.rawQueryString}")
    }

  def authorizeApply =
    Auth { implicit ctx => me =>
      withPrompt { prompt =>
        prompt.authorize(me, env.oAuth.legacyClientApi.apply) flatMap {
          case Validated.Valid(authorized) =>
            env.oAuth.authorizationApi.create(authorized) map { code =>
              SeeOther(authorized.redirectUrl(code))
            }
          case Validated.Invalid(error) => SeeOther(prompt.redirectUri.error(error, prompt.state)).toFuccess
        }
      }
    }

  private val accessTokenRequestForm =
    import lila.oauth.Protocol.*
    import lila.common.Form.into
    Form(
      mapping(
        "grant_type"    -> optional(text),
        "code"          -> optional(text),
        "code_verifier" -> optional(text),
        "client_id"     -> optional(text.into[ClientId]),
        "redirect_uri"  -> optional(text),
        "client_secret" -> optional(text)
      )(AccessTokenRequest.Raw.apply)(unapply)
    )

  def tokenApply =
    Action.async(parse.form(accessTokenRequestForm)) { implicit req =>
      req.body.prepare match
        case Validated.Valid(prepared) =>
          env.oAuth.authorizationApi.consume(prepared) flatMap {
            case Validated.Valid(granted) =>
              env.oAuth.tokenApi.create(granted) map { token =>
                Ok(
                  Json
                    .obj(
                      "token_type"   -> "Bearer",
                      "access_token" -> token.plain
                    )
                    .add("expires_in" -> token.expires.map(_.getSeconds - nowSeconds))
                )
              }
            case Validated.Invalid(err) => BadRequest(err.toJson).toFuccess
          }
        case Validated.Invalid(err) => BadRequest(err.toJson).toFuccess
    }

  def legacyTokenApply =
    Action.async(parse.form(accessTokenRequestForm)) { implicit req =>
      req.body.prepareLegacy(AccessTokenRequest.BasicAuth from req) match
        case Validated.Valid(prepared) =>
          env.oAuth.authorizationApi.consume(prepared) flatMap {
            case Validated.Valid(granted) =>
              env.oAuth.tokenApi.create(granted) map { token =>
                Ok(
                  Json
                    .obj(
                      "token_type"    -> "Bearer",
                      "access_token"  -> token.plain,
                      "refresh_token" -> s"invalid_for_bc_${ThreadLocalRandom.nextString(17)}"
                    )
                    .add("expires_in" -> token.expires.map(_.getSeconds - nowSeconds))
                )
              }
            case Validated.Invalid(err) => BadRequest(err.toJson).toFuccess
          }
        case Validated.Invalid(err) => BadRequest(err.toJson).toFuccess
    }

  def tokenRevoke =
    Scoped() { implicit req => _ =>
      HTTPRequest.bearer(req) ?? { token =>
        env.oAuth.tokenApi.revoke(token) inject NoContent
      }
    }

  private val revokeClientForm = Form(single("origin" -> text))

  def revokeClient =
    AuthBody { implicit ctx => me =>
      given play.api.mvc.Request[?] = ctx.body
      revokeClientForm
        .bindFromRequest()
        .fold(
          _ => BadRequest.toFuccess,
          origin => env.oAuth.tokenApi.revokeByClientOrigin(origin, me) inject NoContent
        )
    }

  def challengeTokens =
    ScopedBody(_.Web.Mod) { implicit req => me =>
      if (isGranted(_.ApiChallengeAdmin, me))
        lila.oauth.OAuthTokenForm.adminChallengeTokens
          .bindFromRequest()
          .fold(
            err => BadRequest(apiFormError(err)).toFuccess,
            data =>
              env.oAuth.tokenApi.adminChallengeTokens(data, me).map { tokens =>
                JsonOk(tokens.view.mapValues(_.plain).toMap)
              }
          )
      else Unauthorized(jsonError("Missing permission")).toFuccess
    }

  private val testTokenRateLimit = lila.memo.RateLimit[IpAddress](
    credits = 10_000,
    duration = 10.minutes,
    key = "api.token.test"
  )
  def testTokens =
    Action.async(parse.tolerantText) { req =>
      val bearers = Bearer from req.body.split(',').view.take(1000).toList
      testTokenRateLimit[Fu[Api.ApiResult]](req.ipAddress, cost = bearers.size) {
        env.oAuth.tokenApi.test(bearers) map { tokens =>
          import lila.common.Json.given
          ApiResult.Data(JsObject(tokens.map { case (bearer, token) =>
            bearer.value -> token.fold[JsValue](JsNull) { t =>
              Json.obj(
                "userId"  -> t.userId,
                "scopes"  -> t.scopes.map(_.key).mkString(","),
                "expires" -> t.expires
              )
            }
          }))
        }
      }(fuccess(ApiResult.Limited)) map apiC.toHttp
    }
