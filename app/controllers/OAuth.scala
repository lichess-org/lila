package controllers

import views._

import lila.app._
import lila.oauth.AuthenticationRequest

final class OAuth(env: Env) extends LilaController(env) {

  //private val tokenApi = env.oAuth.tokenApi

  def authorize =
    Open { implicit ctx =>
      val request = AuthenticationRequest.Raw(
        responseType = get("response_type", ctx.req),
        redirectUri = get("redirect_uri", ctx.req),
        state = get("state", ctx.req),
        codeChallenge = get("code_challenge", ctx.req),
        codeChallengeMethod = get("code_challenge_method", ctx.req),
        scope = get("scope", ctx.req)
      )
      ctx.me.fold(Redirect(routes.Auth.login).fuccess) { me =>
        Ok("hello").fuccess
      }
    }
}
