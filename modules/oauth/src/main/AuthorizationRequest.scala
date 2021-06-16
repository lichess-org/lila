package lila.oauth

import java.net.URLEncoder
import cats.data.Validated
import com.roundeights.hasher.Algo
import io.lemonlabs.uri.AbsoluteUrl
import org.joda.time.DateTime
import ornicar.scalalib.Random
import play.api.libs.json._

import lila.user.User

object AuthorizationRequest {
  import Protocol._

  case class Raw(
      clientId: Option[String],
      state: Option[String],
      redirectUri: Option[String],
      responseType: Option[String],
      codeChallenge: Option[String],
      codeChallengeMethod: Option[String],
      scope: Option[String]
  ) {
    // In order to show a prompt and redirect back with error codes a valid
    // redirect_uri is absolutely required. Ignore all other errors for now.
    def prompt: Validated[Error, Prompt] = {
      redirectUri
        .toValid(Error.RedirectUriRequired)
        .andThen(RedirectUri.from)
        .map { redirectUri =>
          Prompt(
            redirectUri,
            state.map(State.apply),
            clientId = clientId,
            responseType = responseType,
            codeChallenge = codeChallenge,
            codeChallengeMethod = codeChallengeMethod,
            scope = scope
          )
        }
    }
  }

  case class Prompt(
      redirectUri: RedirectUri,
      state: Option[State],
      clientId: Option[String],
      responseType: Option[String],
      codeChallenge: Option[String],
      codeChallengeMethod: Option[String],
      scope: Option[String]
  ) {
    def errorUrl(error: Error) = redirectUri.error(error, state)

    def cancelUrl = errorUrl(Error.AccessDenied)

    private def validScopes: Validated[Error, List[OAuthScope]] =
      (~scope).split("\\s+").foldLeft(Validated.valid[Error, List[OAuthScope]](List.empty[OAuthScope])) {
        case (acc, key) =>
          acc.andThen { valid =>
            OAuthScope.byKey.get(key).toValid(Error.InvalidScope(key)).map(_ :: valid)
          }
      }

    def maybeScopes: List[OAuthScope] = validScopes.getOrElse(Nil)

    def authorize(user: User): Validated[Error, Authorized] = {
      for {
        clientId      <- clientId.map(ClientId.apply).toValid(Error.ClientIdRequired)
        scopes        <- validScopes
        codeChallenge <- codeChallenge.map(CodeChallenge.apply).toValid(Error.CodeChallengeRequired)
        responseType  <- responseType.toValid(Error.ResponseTypeRequired).andThen(ResponseType.from)
        codeChallengeMethod <- codeChallengeMethod
          .toValid(Error.CodeChallengeMethodRequired)
          .andThen(CodeChallengeMethod.from)
      } yield Authorized(
        clientId,
        redirectUri,
        state,
        codeChallenge,
        codeChallengeMethod,
        user.id,
        scopes
      )
    }
  }

  case class Authorized(
      clientId: ClientId,
      redirectUri: RedirectUri,
      state: Option[State],
      codeChallenge: CodeChallenge,
      codeChallengeMethod: CodeChallengeMethod,
      user: User.ID,
      scopes: List[OAuthScope]
  ) {
    def redirectUrl(code: AuthorizationCode) = redirectUri.code(code, state)
  }
}
