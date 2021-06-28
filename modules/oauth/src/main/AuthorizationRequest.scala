package lila.oauth

import java.net.URLEncoder
import cats.data.Validated
import com.roundeights.hasher.Algo
import io.lemonlabs.uri.AbsoluteUrl
import org.joda.time.DateTime
import play.api.libs.json._

import lila.user.User

object AuthorizationRequest {
  import Protocol._

  case class Raw(
      clientId: Option[String],
      state: Option[String],
      redirectUri: Option[String],
      responseType: Option[String],
      codeChallengeMethod: Option[String],
      codeChallenge: Option[String],
      scope: Option[String]
  ) {
    // In order to show a prompt and redirect back with error codes a valid
    // redirect_uri is absolutely required. Ignore all other errors for now.
    def prompt: Validated[Error, Prompt] =
      for {
        redirectUri <- redirectUri.toValid(Error.RedirectUriRequired).andThen(RedirectUri.from)
        clientId    <- clientId.map(ClientId).toValid(Error.ClientIdRequired)
      } yield Prompt(
        redirectUri,
        state.map(State.apply),
        clientId = clientId,
        responseType = responseType,
        codeChallengeMethod = codeChallengeMethod,
        codeChallenge = codeChallenge,
        scope = scope
      )
  }

  case class Prompt(
      redirectUri: RedirectUri,
      state: Option[State],
      clientId: ClientId,
      responseType: Option[String],
      codeChallengeMethod: Option[String],
      codeChallenge: Option[String],
      scope: Option[String]
  ) {
    def errorUrl(error: Error) = redirectUri.error(error, state)

    def cancelUrl = errorUrl(Error.AccessDenied)

    private def validScopes: Validated[Error, List[OAuthScope]] =
      (~scope)
        .split(" ")
        .filter(_ != "")
        .foldLeft(Validated.valid[Error, List[OAuthScope]](List.empty[OAuthScope])) { case (acc, key) =>
          acc.andThen { valid =>
            OAuthScope.byKey.get(key).toValid(Error.InvalidScope(key)).map(_ :: valid)
          }
        }

    def maybeScopes: List[OAuthScope] = validScopes.getOrElse(Nil)

    def maybeLegacy: Boolean = codeChallengeMethod.isEmpty && codeChallenge.isEmpty

    def authorize(
        user: User,
        legacy: ClientId => Fu[Option[LegacyClientApi.HashedClientSecret]]
    ): Fu[Validated[Error, Authorized]] =
      (codeChallengeMethod match {
        case None =>
          legacy(clientId).dmap(
            _.toValid[Error](Error.CodeChallengeMethodRequired).map(Left.apply)
          )
        case Some(method) =>
          fuccess(CodeChallengeMethod.from(method).andThen { _ =>
            codeChallenge.map(CodeChallenge).toValid[Error](Error.CodeChallengeRequired).map(Right.apply)
          })
      }) dmap { challenge =>
        for {
          challenge    <- challenge
          scopes       <- validScopes
          responseType <- responseType.toValid(Error.ResponseTypeRequired).andThen(ResponseType.from)
        } yield Authorized(
          clientId,
          redirectUri,
          state,
          user.id,
          scopes,
          challenge
        )
      }
  }

  case class Authorized(
      clientId: ClientId,
      redirectUri: RedirectUri,
      state: Option[State],
      user: User.ID,
      scopes: List[OAuthScope],
      challenge: Either[LegacyClientApi.HashedClientSecret, CodeChallenge]
  ) {
    def redirectUrl(code: AuthorizationCode) = redirectUri.code(code, state)
  }
}
