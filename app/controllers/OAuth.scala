package controllers

import views._

import play.api.mvc._
import play.api.libs.json.Json
import cats.data.Validated
import lila.app._
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

  def authorize =
    Open { implicit ctx =>
      reqToAutenticationRequest(ctx.req).prompt match {
        case Validated.Valid(prompt)  => Ok(html.oAuth.app.authorize(prompt)).fuccess
        case Validated.Invalid(error) => BadRequest(Json.obj(
          "error" -> error.error,
          "error_description" -> error.description
        )).fuccess
      }
    /* ctx.me.fold(Redirect(routes.Auth.login).fuccess) { me =>
        Ok("hello").fuccess
      } */
    }
}
