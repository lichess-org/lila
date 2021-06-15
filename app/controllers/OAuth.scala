package controllers

import views._

import play.api.mvc._
import play.api.libs.json.Json
import cats.data.Validated
import scalatags.Text.all.stringFrag
import lila.app._
import lila.api.Context
import lila.oauth.AuthorizationRequest

final class OAuth(env: Env) extends LilaController(env) {

  //private val tokenApi = env.oAuth.tokenApi

  private def reqToAuthorizationRequest(req: RequestHeader) =
    AuthorizationRequest.Raw(
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
        prompt.authorize match {
          case Validated.Valid(authorized @ _) => ???
          case Validated.Invalid(error)        => Redirect(error.redirectUrl(prompt.redirectUri)).fuccess
        }
      }
    }
}
