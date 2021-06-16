package controllers

import views._

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import cats.data.Validated
import scalatags.Text.all.stringFrag
import lila.app._
import lila.api.Context
import lila.oauth.{ AccessTokenRequest, AuthorizationRequest }

final class OAuth(env: Env) extends LilaController(env) {

  private def reqToAuthorizationRequest(req: RequestHeader) =
    AuthorizationRequest.Raw(
      clientId = get("client_id", req),
      responseType = get("response_type", req),
      redirectUri = get("redirect_uri", req),
      state = get("state", req),
      codeChallenge = get("code_challenge", req),
      codeChallengeMethod = get("code_challenge_method", req),
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
          Ok(html.oAuth.app.authorize(prompt))
        })
      }
    }

  def authorizeApply =
    Auth { implicit ctx => me =>
      withPrompt { prompt =>
        prompt.authorize(me) match {
          case Validated.Valid(authorized) =>
            env.oAuth.authorizationApi.create(authorized) map { code =>
              Redirect(authorized.redirectUrl(code))
            }
          case Validated.Invalid(error) => Redirect(prompt.redirectUri.error(error, prompt.state)).fuccess
        }
      }
    }

  val accessTokenRequestForm = Form(
    mapping(
      "grant_type"    -> optional(text),
      "code"          -> optional(text),
      "code_verifier" -> optional(text),
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
              // TODO: This is a hack using the personal access token API.
              val token = lila.oauth.OAuthForm.token
                .Data(
                  description = granted.redirectUri.appOrigin,
                  scopes = granted.scopes.map(_.key)
                )
                .fake(granted.userId)
              env.oAuth.tokenApi.create(token) inject Ok(
                Json.obj(
                  "access_token" -> token.id.value,
                  "token_type"   -> "bearer"
                )
              )
            case Validated.Invalid(err) => BadRequest(err.toJson).fuccess
          }
        case Validated.Invalid(err) => BadRequest(err.toJson).fuccess
      }
    }
}
