package lila.oauth

import cats.data.Validated

object AuthenticationRequest {
  case class Error(error: String, description: String, state: Option[String])

  case class Raw(
  state: Option[String],
  redirectUri: Option[String],
  responseType: Option[String],
  codeChallenge: Option[String],
  codeChallengeMethod: Option[String],
  scope: Option[String]) {
    def validate: Validated[Error, Prepared] = {
      for {
        redirectUri <- redirectUri.toValid(Error("invalid_request", "redirect_uri required", state))
        _ <-
          responseType.toValid(Error("invalid_request", "response_type required", state))
            .ensure(Error("invalid_request", "supports only response_type 'code'", state))(_ == "code")
        codeChallenge <- codeChallenge.toValid(Error("invalid_request", "code_challenge required", state))
        _ <-
          codeChallengeMethod.toValid(Error("invalid_request", "code_challenge_method required", state))
            .ensure(Error("invalid_request", "supports only code_challenge_method 'S256'", state))(_ == "S256")
      } yield Prepared(redirectUri, state, codeChallenge, Nil)
    }
  }

  case class Prepared(
    redirectUri: String,
    state: Option[String],
    codeChallenge: String,
    scopes: List[OAuthScope]
    )
}
