package lila.oauth

import cats.data.Validated
import io.lemonlabs.uri.AbsoluteUrl
import org.joda.time.DateTime
import ornicar.scalalib.Random
import play.api.libs.json._

import lila.user.User

object AuthorizationRequest {
  case class Error(error: String, description: String, state: Option[String]) {
    def redirectUrl(base: AbsoluteUrl) = base.withQueryString(
      "error" -> Some(error),
      "error_description" -> Some(description),
      "state" -> state,
      ).toString
  }
  private object Error {
    def accessDenied(state: Option[String]) = Error("access_denied", "user cancelled authorization", state)
    def invalidRequest(description: String, state: Option[String]) = Error("invalid_request", description, state)
    def unauthorizedClient(description: String, state: Option[String]) = Error("unauthorized_client", description, state)
    def unsupportedResponseType(description: String, state: Option[String]) = Error("unsupported_response_type", description, state)
    def invalidScope(description: String, state: Option[String]) = Error("invalid_scope", description, state)
  }

  case class Raw(
    clientId: Option[String],
    state: Option[String],
    redirectUri: Option[String],
    responseType: Option[String],
    codeChallenge: Option[String],
    codeChallengeMethod: Option[String],
    scope: Option[String]
  ) {
    // In order to show a prompt and redirect back with error codes
    // valid redirect_uri is absolutely required.
    // Ignore all other errors for now.
    def prompt: Validated[Error, Prompt] = {
      redirectUri
        .toValid(Error.invalidRequest("redirect_uri required", None))
        .flatMap(AbsoluteUrl.parseOption(_).toValid(Error.invalidRequest("redirect_uri invalid", None)))
        .map { redirectUri =>
          Prompt(
            clientId=clientId,
            state=state,
            redirectUri=redirectUri,
            responseType=responseType,
            codeChallenge=codeChallenge,
            codeChallengeMethod=codeChallengeMethod,
            scope=scope,
          )
        }
    }
  }

  case class Prompt(
    clientId: Option[String],
    state: Option[String],
    redirectUri: AbsoluteUrl,
    responseType: Option[String],
    codeChallenge: Option[String],
    codeChallengeMethod: Option[String],
    scope: Option[String],
  ) {
    def humanReadableOrigin: String = {
      if (redirectUri.hostOption.map(_.value).has("localhost") && List("http", "ionic", "capacitor").has(redirectUri.scheme))
        "localhost"
      else if (redirectUri.scheme == "https")
        redirectUri.apexDomain getOrElse redirectUri.hostOption.fold(redirectUri.toString)(_.value)
      else
        s"${redirectUri.scheme}://..." // untrusted or insecure scheme
    }

    def cancelUrl: String = Error.accessDenied(state).redirectUrl(redirectUri)

    private def validateScopes: (List[String], List[OAuthScope]) =
      (~scope).split("\\s+").foldLeft(List.empty[String] -> List.empty[OAuthScope]) {
        case ((invalid, valid), key) =>
          OAuthScope.byKey.get(key) match {
            case Some(scope) => invalid -> (scope :: valid)
            case None => (key :: invalid) -> valid
          }
      }

    def scopes = validateScopes._2

    def authorize(user: User): Validated[Error, Authorized] = {
      val (invalidScopes, validScopes) = validateScopes
      for {
        clientId <- clientId.toValid(Error.invalidRequest("client_id required", state))
        scopes <- invalidScopes.headOption match {
          case None => Validated.valid(validScopes)
          case Some(key) => Validated.invalid(Error.invalidScope(s"invalid scope: $key", state))
        }
        codeChallenge <- codeChallenge.toValid(Error.invalidRequest("code_challenge required", state))
        _ <-
          responseType.toValid(Error.invalidRequest("response_type required", state))
            .ensure(Error.unsupportedResponseType("supports only response_type 'code'", state))(_ == "code")
        _ <-
          codeChallengeMethod.toValid(Error.invalidRequest("code_challenge_method required", state))
            .ensure(Error.unauthorizedClient("supports only code_challenge_method 'S256'", state))(_ == "S256")
      } yield Authorized(
        clientId=clientId,
        redirectUri=redirectUri,
        state=state,
        codeChallenge=codeChallenge,
        user=user.id,
        scopes=scopes,
        nonce=Random.secureString(16),
        expires=DateTime.now().plusSeconds(120)
      )
    }
  }

  case class Authorized(
    clientId: String,
    redirectUri: AbsoluteUrl,
    state: Option[String],
    codeChallenge: String,
    user: User.ID,
    scopes: List[OAuthScope],
    expires: DateTime,
    nonce: String,
  ) {
    def insecureCode = Json.obj(
      "client_id" -> clientId,
      "redirect_uri" -> redirectUri.toString,
      "code_challenge" -> codeChallenge,
      "user" -> user,
      "scopes" -> scopes.map(_.key),
      "expires" -> expires.toString,
      "nonce" -> nonce,
    ).toString

    def redirectUrl: String = redirectUri.withQueryString(
      "code" -> Some(insecureCode),
      "state" -> state,
    ).toString
  }
}
