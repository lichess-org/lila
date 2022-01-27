package lila.oauth

import java.util.Base64
import scala.util.Try
import cats.data.Validated
import play.api.libs.json.Json
import com.roundeights.hasher.Algo
import io.mola.galimatias.{ StrictErrorHandler, URL, URLParsingSettings }

import lila.common.SecureRandom
import lila.common.String.urlencode

object Protocol {
  case class AuthorizationCode(secret: String) extends AnyVal {
    def hashed            = Algo.sha256(secret).hex
    override def toString = "AuthorizationCode(***)"
  }
  object AuthorizationCode {
    def random() = AuthorizationCode(s"liu_${SecureRandom.nextString(32)}")
  }

  case class ClientId(value: String) extends AnyVal

  case class State(value: String) extends AnyVal

  case class CodeChallengeMethod()
  object CodeChallengeMethod {
    def from(codeChallengeMethod: String): Validated[Error, CodeChallengeMethod] =
      codeChallengeMethod match {
        case "S256" => Validated.valid(CodeChallengeMethod())
        case _      => Validated.invalid(Error.UnsupportedCodeChallengeMethod)
      }
  }

  case class CodeChallenge(value: String) extends AnyVal

  case class CodeVerifier(value: String) extends AnyVal {
    def matches(challenge: CodeChallenge) =
      Base64.getUrlEncoder().withoutPadding().encodeToString(Algo.sha256(value).bytes) == challenge.value
  }
  object CodeVerifier {
    def from(value: String): Validated[Error, CodeVerifier] =
      Validated
        .valid(value)
        .ensure(Error.CodeVerifierTooShort)(_.size >= 43)
        .map(CodeVerifier.apply)
  }

  case class ResponseType()
  object ResponseType {
    def from(responseType: String): Validated[Error, ResponseType] =
      responseType match {
        case "code" => Validated.valid(ResponseType())
        case _      => Validated.invalid(Error.UnsupportedResponseType)
      }
  }

  case class GrantType()
  object GrantType {
    def from(grantType: String): Validated[Error, GrantType] =
      grantType match {
        case "authorization_code" => Validated.valid(GrantType())
        case _                    => Validated.invalid(Error.UnsupportedGrantType)
      }
  }

  case class RedirectUri(value: URL) extends AnyVal {
    private def host: Option[String] = Option(value.host).map(_.toString)

    // https://github.com/smola/galimatias/issues/72 will be more precise
    def clientOrigin: String = s"${value.scheme}://${~host}"

    def insecure =
      value.scheme == "http" && !host.exists(h => List("localhost", "[::1]", "127.0.0.1").has(h))

    def withoutQuery: String = value.withQuery(null).toString

    def error(error: Error, state: Option[State]): String = value
      .withQuery(
        s"error=${urlencode(error.error)}&error_description=${urlencode(error.description)}&state=${urlencode(~state.map(_.value))}"
      )
      .toString

    def code(code: AuthorizationCode, state: Option[State]): String = value
      .withQuery(
        s"code=${urlencode(code.secret)}&state=${urlencode(~state.map(_.value))}"
      )
      .toString

    def matches(other: UncheckedRedirectUri) = value.toString == other.value
  }
  object RedirectUri {
    def from(redirectUri: String): Validated[Error, RedirectUri] =
      Try {
        URL.parse(URLParsingSettings.create.withErrorHandler(StrictErrorHandler.getInstance), redirectUri)
      }.toOption
        .toValid(Error.RedirectUriInvalid)
        .ensure(Error.RedirectSchemeNotAllowed)(url =>
          List(
            // standard
            "http",
            "https",
            "ionic",
            "capacitor",
            // bc
            "squareoffapp",
            "anichess",
            "lichessmac",
            "chessrtx",
            "chesscomopse",
            "tectootb"
          ).has(url.scheme) || url.scheme.contains(".")
        )
        .map(RedirectUri.apply)

    def unchecked(trusted: String): RedirectUri = RedirectUri(URL.parse(trusted))
  }

  case class UncheckedRedirectUri(value: String) extends AnyVal

  sealed abstract class Error(val error: String) {
    def description: String
    def toJson = Json.obj(
      "error"             -> error,
      "error_description" -> description
    )
  }
  object Error {
    case object AccessDenied extends Error("access_denied") {
      def description = "user cancelled authorization"
    }
    case object UnsupportedResponseType extends Error("unsupported_response_type") {
      val description = "supports only response_type 'code'"
    }
    case object UnsupportedGrantType extends Error("unsupported_grant_type") {
      val description = "supports only grant_type 'authorization_code'"
    }

    abstract class InvalidRequest(val description: String) extends Error("invalid_request")
    case object ClientIdRequired                           extends InvalidRequest("client_id required (choose any)")
    case object RedirectUriRequired                        extends InvalidRequest("redirect_uri required")
    case object RedirectUriInvalid                         extends InvalidRequest("redirect_uri invalid")
    case object RedirectSchemeNotAllowed
        extends InvalidRequest(
          "exotic redirect_uri scheme not allowed (use reverse domain notation like com.example:// for custom applications)"
        )
    case object ResponseTypeRequired        extends InvalidRequest("response_type required")
    case object CodeChallengeRequired       extends InvalidRequest("code_challenge required")
    case object CodeChallengeMethodRequired extends InvalidRequest("code_challenge_method required")
    case object GrantTypeRequired           extends InvalidRequest("grant_type required")
    case object CodeRequired                extends InvalidRequest("code required")
    case object CodeVerifierRequired        extends InvalidRequest("code_verifier required")
    case object CodeVerifierTooShort        extends InvalidRequest("code_verifier too short")

    case class InvalidScope(val key: String) extends Error("invalid_scope") {
      def description = s"invalid scope: ${urlencode(key)}"
    }

    abstract class UnauthorizedClient(val description: String) extends Error("unauthorized_client")
    case object UnsupportedCodeChallengeMethod
        extends UnauthorizedClient("supports only code_challenge_method 'S256'")

    abstract class InvalidGrant(val description: String) extends Error("invalid_grant")
    case object AuthorizationCodeInvalid
        extends InvalidGrant("authorization code invalid, expired or consumed")
    case object AuthorizationCodeExpired extends InvalidGrant("authorization code expired")
    case object MismatchingRedirectUri
        extends InvalidGrant("authorization code was issued for a different redirect_uri")
    case object MismatchingClient
        extends InvalidGrant("authorization code was issued for a different client_Id")
    case object MismatchingCodeVerifier
        extends InvalidGrant("hash of code_verifier does not match code_challenge")
  }
}
