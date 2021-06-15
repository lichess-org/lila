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
      parseRedirectUri(redirectUri).map { Prompt(_, scopes, state) }
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
    scopes: List[OAuthScope],
    state: Option[String]
  ) {
    def humanReadableOrigin: String = {
      if (redirectUri.hostOption.map(_.value).has("localhost") && List("http", "ionic", "capacitor").has(redirectUri.scheme))
        "localhost"
      else if (redirectUri.scheme == "https")
        redirectUri.apexDomain getOrElse redirectUri.hostOption.fold(redirectUri.toString)(_.value)
      else
        s"${redirectUri.scheme}://..." // untrusted or insecure scheme
    }

    def cancelHref: String = redirectUri.withQueryString(
      "error" -> Some("access_denied"),
      "error_description" -> Some("user cancelled authorization"),
      "state" -> state
      ).toString
  }

  case class Prepared(
    redirectUri: AbsoluteUrl,
    state: Option[String],
    codeChallenge: String,
    scopes: List[OAuthScope]
    )
}
