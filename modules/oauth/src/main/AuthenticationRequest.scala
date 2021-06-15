package lila.oauth

import cats.data.Validated
import io.lemonlabs.uri.AbsoluteUrl

object AuthenticationRequest {
  case class Error(error: String, description: String, state: Option[String])

  def parseRedirectUri(uri: Option[String]): Validated[Error, AbsoluteUrl] = 
    for {
      uri <- uri.toValid(Error("invalid_request", "redirect_uri required", None))
      uri <- AbsoluteUrl.parseOption(uri).toValid(Error("invalid_request", "redirect_uri invalid", None))
    } yield uri

  case class Raw(
  state: Option[String],
  redirectUri: Option[String],
  responseType: Option[String],
  codeChallenge: Option[String],
  codeChallengeMethod: Option[String],
  scope: Option[String]) {
    def prompt: Validated[Error, Prompt] = {
      // In order to show a prompt and redirect back with error codes
      // a valid redirect_uri is absolutely required. Ignore all other errors
      // for now.
      val scopes = scope ?? { scope => scope.split("\\s+").toList.flatMap(OAuthScope.byKey.get) }
      parseRedirectUri(redirectUri).map { Prompt(_, scopes) }
    }

    def validate: Validated[Error, Prepared] = {
      for {
        redirectUri <- parseRedirectUri(redirectUri)
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

  case class Prompt(
    redirectUri: AbsoluteUrl,
    scopes: List[OAuthScope]
  ) {
    def shortName: String = {
      if (redirectUri.scheme == "http" || redirectUri.scheme == "https")
        redirectUri.apexDomain getOrElse redirectUri.hostOption.fold("???")(_.normalize.toStringPunycode)
      else
        s"${redirectUri.scheme}://"
    }

    def cancel: String = redirectUri.toString
  }

  case class Prepared(
    redirectUri: AbsoluteUrl,
    state: Option[String],
    codeChallenge: String,
    scopes: List[OAuthScope]
    )
}
