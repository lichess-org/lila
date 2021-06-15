package controllers

import views._

import play.api.mvc._
import play.api.libs.json.Json
import cats.data.Validated
import scalatags.Text.all.stringFrag
import lila.app._
import lila.api.Context
import lila.oauth.AuthenticationRequest

final class OAuth(env: Env) extends LilaController(env) {

  //private val tokenApi = env.oAuth.tokenApi

  private def reqToAutenticationRequest(req: RequestHeader) =
    AuthenticationRequest.Raw(
      responseType = get("response_type", req),
      redirectUri = get("redirect_uri", req),
      state = get("state", req),
      codeChallenge = get("code_challenge", req),
      codeChallengeMethod = get("code_challenge_method", req),
      scope = get("scope", req)
    )

  private def badRequest(error: AuthenticationRequest.Error)(implicit ctx: Context) =
    BadRequest(html.site.message("Bad authorization request")(stringFrag(error.description)))

  def authorize =
    Open { implicit ctx =>
      fuccess(reqToAutenticationRequest(ctx.req).prompt match {
        case Validated.Valid(prompt)  =>
          ctx.me.fold(Redirect(routes.Auth.login.url, Map("referrer" -> ctx.req.uri.some))) { me =>
            Ok(html.oAuth.app.authorize(prompt))
          }
        case Validated.Invalid(error) => badRequest(error)
      })
    }
}
