package controllers

import cats.data.Validated
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc._
import scalatags.Text.all.stringFrag
import views._

import lila.api.Context
import lila.common.HTTPRequest
import lila.app._
import lila.oauth.{ AccessToken, AccessTokenRequest, AuthorizationRequest }

final class OAuth(env: Env) extends LilaController(env) {

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
      "client_secret" -> optional(text),
      "redirect_uri"  -> optional(text),
      "client_id"     -> optional(text)
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
                      "access_token" -> token.id.value
                    )
                    .add("expires_in" -> token.expires.map(_.getSeconds - nowSeconds))
                )
              }
            case Validated.Invalid(err) => BadRequest(err.toJson).fuccess
          }
        case Validated.Invalid(err) => BadRequest(err.toJson).fuccess
      }
    }

  def legacyTokenApply = tokenApply

  def tokenRevoke =
    Scoped() { implicit req => _ =>
      HTTPRequest.bearer(req) ?? { token =>
        env.oAuth.tokenApi.revoke(AccessToken.Id(token)) map env.oAuth.server.deleteCached inject NoContent
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
          origin =>
            env.oAuth.tokenApi.revokeByClientOrigin(origin, me) map {
              _ foreach { token =>
                env.oAuth.server.deleteCached(token)
              }
            } inject NoContent
        )
    }
}
