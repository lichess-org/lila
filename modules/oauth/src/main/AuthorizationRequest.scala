package lila.oauth

import cats.data.Validated

import lila.user.User

object AuthorizationRequest:
  import Protocol.*

  case class Raw(
      clientId: Option[ClientId],
      state: Option[State],
      redirectUri: Option[String],
      responseType: Option[String],
      codeChallengeMethod: Option[String],
      codeChallenge: Option[CodeChallenge],
      scope: Option[String],
      username: Option[UserStr]
  ):
    // In order to show a prompt and redirect back with error codes a valid
    // redirect_uri is absolutely required. Ignore all other errors for now.
    def prompt: Validated[Error, Prompt] =
      for {
        redirectUri <- redirectUri.toValid(Error.RedirectUriRequired).andThen(RedirectUri.from)
        clientId    <- clientId.toValid(Error.ClientIdRequired)
      } yield Prompt(
        redirectUri,
        state,
        clientId = clientId,
        responseType = responseType,
        codeChallengeMethod = codeChallengeMethod,
        codeChallenge = codeChallenge,
        scope = scope,
        userId = username.map(_.id)
      )

  case class Prompt(
      redirectUri: RedirectUri,
      state: Option[State],
      clientId: ClientId,
      responseType: Option[String],
      codeChallengeMethod: Option[String],
      codeChallenge: Option[CodeChallenge],
      scope: Option[String],
      userId: Option[UserId]
  ):
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
        legacy: (ClientId, RedirectUri) => Fu[Option[LegacyClientApi.HashedClientSecret]]
    ): Fu[Validated[Error, Authorized]] =
      (codeChallengeMethod match {
        case None =>
          legacy(clientId, redirectUri).dmap(
            _.toValid[Error](Error.CodeChallengeMethodRequired).map(Left.apply)
          )
        case Some(method) =>
          fuccess(CodeChallengeMethod.from(method).andThen { _ =>
            codeChallenge
              .toValid[Error](Error.CodeChallengeRequired)
              .map(Right.apply)
          })
      }) dmap { challenge =>
        for {
          challenge <- challenge
          scopes    <- validScopes
          _         <- responseType.toValid(Error.ResponseTypeRequired).andThen(ResponseType.from)
        } yield Authorized(
          clientId,
          redirectUri,
          state,
          user.id,
          scopes,
          challenge
        )
      }

  case class Authorized(
      clientId: ClientId,
      redirectUri: RedirectUri,
      state: Option[State],
      user: UserId,
      scopes: List[OAuthScope],
      challenge: Either[LegacyClientApi.HashedClientSecret, CodeChallenge]
  ):
    def redirectUrl(code: AuthorizationCode) = redirectUri.code(code, state)
