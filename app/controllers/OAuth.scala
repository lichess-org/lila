package controllers

import cats.data.Validated
import org.joda.time.DateTime
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{ JsNull, JsObject, JsString, JsValue, Json }
import play.api.mvc._
import scala.concurrent.duration._
import scalatags.Text.all.stringFrag
import views._

import lila.api.Context
import lila.app._
import lila.common.{ HTTPRequest, IpAddress }
import lila.oauth.{ AccessToken, AccessTokenRequest, AuthorizationRequest }

final class OAuth(env: Env, apiC: => Api) extends LilaController(env) {

  private def reqToAuthorizationRequest(req: RequestHeader) =
    AuthorizationRequest.Raw(
      clientId = get("client_id", req),
      responseType = get("response_type", req),
      redirectUri = get("redirect_uri", req),
      state = get("state", req),
      codeChallengeMethod = get("code_challenge_method", req),
      codeChallenge = get("code_challenge", req),
      scope = get("scope", req)
    )

  private def withPrompt(f: AuthorizationRequest.Prompt => Fu[Result])(implicit ctx: Context) =
    reqToAuthorizationRequest(ctx.req).prompt match {
      case Validated.Valid(prompt) => f(prompt)
      case Validated.Invalid(error) =>
        BadRequest(html.site.message("Bad authorization request")(stringFrag(error.description))).fuccess
    }

  def authorize =
    Open { implicit ctx =>
      withPrompt { prompt =>
        fuccess(ctx.me.fold(Redirect(routes.Auth.login.url, Map("referrer" -> List(ctx.req.uri)))) { me =>
          Ok(
            html.oAuth.authorize(prompt, me, s"${routes.OAuth.authorizeApply}?${ctx.req.rawQueryString}")
          )
        })
      }
    }

  def legacyAuthorize =
    Action { req =>
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
          case Validated.Invalid(error) => SeeOther(prompt.redirectUri.error(error, prompt.state)).fuccess
        }
      }
    }

  private val accessTokenRequestForm = Form(
    mapping(
      "grant_type"    -> optional(text),
      "code"          -> optional(text),
      "code_verifier" -> optional(text),
      "client_id"     -> optional(text),
      "redirect_uri"  -> optional(text),
      "client_secret" -> optional(text)
    )(AccessTokenRequest.Raw.apply)(AccessTokenRequest.Raw.unapply)
  )

  def tokenApply =
    Action.async(parse.form(accessTokenRequestForm)) { implicit req =>
      req.body.prepare match {
        case Validated.Valid(prepared) =>
          env.oAuth.authorizationApi.consume(prepared) flatMap {
            case Validated.Valid(granted) =>
              env.oAuth.tokenApi.create(granted) map { token =>
                Ok(
                  Json
                    .obj(
                      "token_type"   -> "Bearer",
                      "access_token" -> token.plain.secret
                    )
                    .add("expires_in" -> token.expires.map(_.getSeconds - nowSeconds))
                )
              }
            case Validated.Invalid(err) => BadRequest(err.toJson).fuccess
          }
        case Validated.Invalid(err) => BadRequest(err.toJson).fuccess
      }
    }

  def legacyTokenApply =
    Action.async(parse.form(accessTokenRequestForm)) { implicit req =>
      req.body.prepareLegacy(AccessTokenRequest.BasicAuth from req) match {
        case Validated.Valid(prepared) =>
          env.oAuth.authorizationApi.consume(prepared) flatMap {
            case Validated.Valid(granted) =>
              env.oAuth.tokenApi.create(granted) map { token =>
                Ok(
                  Json
                    .obj(
                      "token_type"    -> "Bearer",
                      "access_token"  -> token.plain.secret,
                      "refresh_token" -> s"invalid_for_bc_${lila.common.ThreadLocalRandom.nextString(17)}"
                    )
                    .add("expires_in" -> token.expires.map(_.getSeconds - nowSeconds))
                )
              }
            case Validated.Invalid(err) => BadRequest(err.toJson).fuccess
          }
        case Validated.Invalid(err) => BadRequest(err.toJson).fuccess
      }
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
      implicit def body = ctx.body
      revokeClientForm
        .bindFromRequest()
        .fold(
          _ => BadRequest.fuccess,
          origin => env.oAuth.tokenApi.revokeByClientOrigin(origin, me) inject NoContent
        )
    }

  def challengeTokens =
    ScopedBody(_.Web.Mod) { implicit req => me =>
      if (isGranted(_.ApiChallengeAdmin, me))
        lila.oauth.OAuthTokenForm.adminChallengeTokens
          .bindFromRequest()
          .fold(
            err => BadRequest(apiFormError(err)).fuccess,
            data =>
              env.oAuth.tokenApi.adminChallengeTokens(data, me).map { tokens =>
                JsonOk(tokens.view.mapValues(t => t.plain.secret).toMap)
              }
          )
      else Unauthorized(jsonError("Missing permission")).fuccess
    }

  private val testTokenRateLimit = new lila.memo.RateLimit[IpAddress](
    credits = 10_000,
    duration = 10.minutes,
    key = "api.token.test"
  )
  def testTokens =
    Action.async(parse.tolerantText) { req =>
      val bearers = req.body.split(',').view.take(1000).toList
      testTokenRateLimit[Fu[Api.ApiResult]](HTTPRequest ipAddress req, cost = bearers.size) {
        env.oAuth.tokenApi.test(bearers map lila.common.Bearer.apply) map { tokens =>
          import lila.common.Json.jodaWrites
          Api.Data(JsObject(tokens.map { case (bearer, token) =>
            bearer.secret -> token.fold[JsValue](JsNull) { t =>
              Json.obj(
                "userId"  -> t.userId,
                "scopes"  -> t.scopes.map(_.key).mkString(","),
                "expires" -> t.expiresOrFarFuture
              )
            }
          }))
        }
      }(fuccess(Api.Limited)) map apiC.toHttp
    }
}
