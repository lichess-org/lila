package lila.oauth

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
    def prompt: Either[Error, Prompt] = for
      redirectUri <- redirectUri.toRight(Error.RedirectUriRequired).flatMap(RedirectUri.from)
      clientId <- clientId.toRight(Error.ClientIdRequired)
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

    private def validScopes: Either[Error, OAuthScopes] =
      (~scope)
        .split(" ")
        .filter(_ != "")
        .toList
        .foldLeftM(List.empty[OAuthScope]): (acc, key) =>
          OAuthScope.byKey.get(key).toRight(Error.InvalidScope(key)).map(_ :: acc)
        .map(OAuthScopes(_))

    def scopes: OAuthScopes = validScopes.getOrElse(OAuthScopes(Nil))

    def maybeLegacy: Boolean = codeChallengeMethod.isEmpty && codeChallenge.isEmpty

    lazy val trusted =
      List("lichess.org", "discotron.lichess.org", "www.lichess4545.com", "wiki.lichess.ovh").has:
        ~redirectUri.host

    lazy val lichessMobileAttributes = List(
      clientId == ClientId("lichess_mobile"),
      redirectUri.value.toString == "org.lichess.mobile://login-callback",
      scope.has(OAuthScope.Web.Mobile.key)
    )
    lazy val looksLikeLichessMobile = lichessMobileAttributes.forall(identity)
    lazy val mimicsLichessMobile = !looksLikeLichessMobile && lichessMobileAttributes.exists(identity)
    lazy val isDanger = scopes.intersects(OAuthScope.dangerList) && !trusted && !looksLikeLichessMobile

    def authorize(
        user: UserId,
        legacy: (ClientId, RedirectUri) => Fu[Option[LegacyClientApi.HashedClientSecret]]
    )(using Executor): FuRaise[Error, Authorized] =
      val challengeFu =
        codeChallengeMethod.match
          case None =>
            legacy(clientId, redirectUri)
              .flatMap(_.raiseIfNone(Error.CodeChallengeMethodRequired))
              .map(Left(_))
          case Some(method) =>
            CodeChallengeMethod
              .check(method)
              .raiseIfSome:
                codeChallenge.raiseIfNone(Error.CodeChallengeMethodRequired).map(Right(_))

      for
        challenge <- challengeFu
        scopes <- validScopes.fold(_.raise, fuccess)
        tpe <- responseType.raiseIfNone(Error.ResponseTypeRequired)
        _ <- ResponseType.check(tpe).raiseIfSome(funit)
      yield Authorized(clientId, redirectUri, state, user, scopes, challenge)

  case class Authorized(
      clientId: ClientId,
      redirectUri: RedirectUri,
      state: Option[State],
      user: UserId,
      scopes: OAuthScopes,
      challenge: Either[LegacyClientApi.HashedClientSecret, CodeChallenge]
  ):
    def redirectUrl(code: AuthorizationCode) = redirectUri.code(code, state)

  def logPrompt(prompt: Prompt, me: Option[UserId])(using req: play.api.mvc.RequestHeader) =
    if prompt.mimicsLichessMobile
    then
      val reqInfo = lila.common.HTTPRequest.print(req)
      logger.warn(s"OAuth prompt looks like lichess mobile: ${me.fold("anon")(_.value)} $reqInfo")
