package lila.oauth

import com.roundeights.hasher.Algo
import io.mola.galimatias.URL
import play.api.libs.json.Json
import scalalib.SecureRandom

import java.util.Base64

import lila.common.String.urlencode

object Protocol:
  case class AuthorizationCode(secret: String) extends AnyVal:
    def hashed            = Algo.sha256(secret).hex
    override def toString = "AuthorizationCode(***)"
  object AuthorizationCode:
    def random() = AuthorizationCode(s"liu_${SecureRandom.nextString(32)}")

  opaque type ClientId = String
  object ClientId extends OpaqueString[ClientId]

  opaque type State = String
  object State extends OpaqueString[State]

  object CodeChallengeMethod:
    def from(codeChallengeMethod: String): Either[Error, Unit] =
      codeChallengeMethod match
        case "S256" => ().asRight
        case _      => Error.UnsupportedCodeChallengeMethod.asLeft

  opaque type CodeChallenge = String
  object CodeChallenge extends OpaqueString[CodeChallenge]

  opaque type CodeVerifier = String
  object CodeVerifier extends OpaqueString[CodeVerifier]:
    extension (a: CodeVerifier)
      def matches(challenge: CodeChallenge) =
        Base64.getUrlEncoder().withoutPadding().encodeToString(Algo.sha256(a.value).bytes) == challenge

    def from(value: String): Either[Error, CodeVerifier] =
      Right(value)
        .ensure(Error.CodeVerifierTooShort)(_.size >= 43)
        .map(CodeVerifier(_))

  object ResponseType:
    def from(responseType: String): Either[Error, Unit] =
      responseType match
        case "code" => ().asRight
        case _      => Error.UnsupportedResponseType.asLeft

  object GrantType:
    def from(grantType: String): Either[Error, Unit] =
      grantType match
        case "authorization_code" => ().asRight
        case _                    => Error.UnsupportedGrantType.asLeft

  case class RedirectUri(value: URL) extends AnyVal:

    def host: Option[String] = Option(value.host).map(_.toHostString)

    // https://github.com/smola/galimatias/issues/72 will be more precise
    def clientOrigin: String = s"${value.scheme}://${~host}"

    def insecure =
      value.scheme == "http" && !host.exists(h =>
        List("localhost", "[::1]", "[::]", "127.0.0.1", "0.0.0.0").has(h)
      )

    def withoutQuery: String = value.withQuery(null).toString

    def error(error: Error, state: Option[State]): String = value
      .withQuery(
        s"error=${urlencode(error.error)}&error_description=${urlencode(error.description)}&state=${urlencode(~state)}"
      )
      .toString

    def code(code: AuthorizationCode, state: Option[State]): String = value
      .withQuery(
        s"code=${urlencode(code.secret)}&state=${urlencode(~state)}"
      )
      .toString

    def matches(other: UncheckedRedirectUri) = value.toString == other.value
  object RedirectUri:
    def from(redirectUri: String): Either[Error, RedirectUri] =
      lila.common.url
        .parse(redirectUri)
        .toEither
        .leftMap(_ => Error.RedirectUriInvalid)
        .ensure(Error.RedirectSchemeNotAllowed): url =>
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
        .map(RedirectUri.apply)

  case class UncheckedRedirectUri(value: String) extends AnyVal

  sealed abstract class Error(val error: String):
    def description: String
    def toJson = Json.obj(
      "error"             -> error,
      "error_description" -> description
    )
  object Error:
    case object AccessDenied extends Error("access_denied"):
      def description = "user cancelled authorization"
    case object UnsupportedResponseType extends Error("unsupported_response_type"):
      val description = "supports only response_type 'code'"
    case object UnsupportedGrantType extends Error("unsupported_grant_type"):
      val description = "supports only grant_type 'authorization_code'"

    abstract class InvalidRequest(val description: String) extends Error("invalid_request")
    case object ClientIdRequired    extends InvalidRequest("client_id required (choose any)")
    case object RedirectUriRequired extends InvalidRequest("redirect_uri required")
    case object RedirectUriInvalid  extends InvalidRequest("redirect_uri invalid")
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

    case class InvalidScope(val key: String) extends Error("invalid_scope"):
      def description = s"invalid scope: ${urlencode(key)}"

    abstract class UnauthorizedClient(val description: String) extends Error("unauthorized_client")
    case object UnsupportedCodeChallengeMethod
        extends UnauthorizedClient("supports only code_challenge_method 'S256'")

    abstract class InvalidGrant(val description: String) extends Error("invalid_grant")
    case object AuthorizationCodeInvalid
        extends InvalidGrant("authorization code invalid, expired or consumed")
    case object AuthorizationCodeExpired extends InvalidGrant("authorization code expired")
    case class MismatchingRedirectUri(url: String)
        extends InvalidGrant(s"authorization code was issued for a different redirect_uri: $url")
    case class MismatchingClient(id: ClientId)
        extends InvalidGrant(s"authorization code was issued for a different client_id: $id")
    case object MismatchingCodeVerifier
        extends InvalidGrant("hash of code_verifier does not match code_challenge")
