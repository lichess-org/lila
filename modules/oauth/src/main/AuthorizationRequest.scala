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
    def prompt: Validated[Error, Prompt] = for
      redirectUri <- redirectUri.toValid(Error.RedirectUriRequired).andThen(RedirectUri.from)
      clientId    <- clientId.toValid(Error.ClientIdRequired)
    yield Prompt(
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

    private def validScopes: Validated[Error, OAuthScopes] =
      (~scope)
        .split(" ")
        .filter(_ != "")
        .foldLeft(Validated.valid[Error, List[OAuthScope]](List.empty[OAuthScope])) { case (acc, key) =>
          acc.andThen: valid =>
            OAuthScope.byKey.get(key).toValid(Error.InvalidScope(key)).map(_ :: valid)
        }
        .map(OAuthScopes(_))

    def scopes: OAuthScopes = validScopes.getOrElse(OAuthScopes(Nil))

    def maybeLegacy: Boolean = codeChallengeMethod.isEmpty && codeChallenge.isEmpty

    lazy val lichessMobileAttributes = List(
      clientId == ClientId("lichess_mobile"),
      redirectUri.value.toString == "org.lichess.mobile://login-callback",
      scope.has(OAuthScope.Web.Mobile.key)
    )

    lazy val looksLikeLichessMobile = lichessMobileAttributes.forall(identity)
    lazy val isDanger               = scopes.intersects(OAuthScope.dangerList) && !looksLikeLichessMobile
    lazy val mimicsLichessMobile    = !looksLikeLichessMobile && lichessMobileAttributes.exists(identity)

    def authorize(
        user: User,
        legacy: (ClientId, RedirectUri) => Fu[Option[LegacyClientApi.HashedClientSecret]]
    ): Fu[Validated[Error, Authorized]] =
      codeChallengeMethod
        .match
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
        .dmap: challenge =>
          for
            challenge <- challenge
            scopes    <- validScopes
            _         <- responseType.toValid(Error.ResponseTypeRequired).andThen(ResponseType.from)
          yield Authorized(
            clientId,
            redirectUri,
            state,
            user.id,
            scopes,
            challenge
          )

  case class Authorized(
      clientId: ClientId,
      redirectUri: RedirectUri,
      state: Option[State],
      user: UserId,
      scopes: OAuthScopes,
      challenge: Either[LegacyClientApi.HashedClientSecret, CodeChallenge]
  ):
    def redirectUrl(code: AuthorizationCode) = redirectUri.code(code, state)

  def logPrompt(prompt: Prompt, me: Option[User])(using req: play.api.mvc.RequestHeader) =
    if prompt.mimicsLichessMobile
    then
      val reqInfo = lila.common.HTTPRequest.print(req)
      logger.warn(s"OAuth prompt looks like lichess mobile: ${me.fold("anon")(_.username)} $reqInfo")
