package lila.oauth

import lila.core.net.ValidReferrer

object AuthorizationRequest:

  import Protocol.*

  def fromReq(using play.api.mvc.RequestHeader): Either[Error, Prompt] =
    import lila.common.HTTPRequest.{ queryStringGetAs as getAs, queryStringGet as get }
    for
      redirectUri <- get("redirect_uri").toRight(Error.RedirectUriRequired).flatMap(RedirectUri.from)
      clientId <- getAs[ClientId]("client_id").toRight(Error.ClientIdRequired)
      scopes <- get("scope").orZero
        .split(" ")
        .filter(_ != "")
        .toList
        .foldLeftM(List.empty[OAuthScope]): (acc, key) =>
          OAuthScope.byKey.get(key).toRight(Error.InvalidScope(key)).map(_ :: acc)
        .map(OAuthScopes(_))
    yield Prompt(
      redirectUri,
      clientId,
      scopes,
      getAs[State]("state"),
      responseType = get("response_type"),
      codeChallengeMethod = get("code_challenge_method"),
      codeChallenge = getAs[CodeChallenge]("code_challenge"),
      userId = getAs[UserStr]("username").flatMap(_.validateId)
    )

  case class PromptSignup(username: UserName, email: EmailAddress)

  def promptSignupFrom(referrer: ValidReferrer): Option[PromptSignup] =
    import lila.common.url.{ parse, queryParam }
    for
      ref <- parse(referrer.value).toOption
      username <- ref.queryParam("default_username").map(UserName(_))
      email <- ref.queryParam("default_email").flatMap(EmailAddress.from)
      sign <- ref.queryParam("default_sign")
      if sign.nonEmpty
    yield PromptSignup(username, email)

  case class Prompt(
      redirectUri: RedirectUri,
      clientId: ClientId,
      scopes: OAuthScopes,
      state: Option[State],
      responseType: Option[String],
      codeChallengeMethod: Option[String],
      codeChallenge: Option[CodeChallenge],
      userId: Option[UserId]
  ):
    def errorUrl(error: Error) = redirectUri.error(error, state)

    def cancelUrl = errorUrl(Error.AccessDenied)

    def maybeLegacy: Boolean = codeChallengeMethod.isEmpty && codeChallenge.isEmpty

    lazy val trusted =
      List("lichess.org", "discotron.lichess.org", "www.lichess4545.com", "wiki.lichess.ovh").has:
        ~redirectUri.host

    lazy val isDanger = scopes.intersects(OAuthScope.dangerList) && !trusted

    def authorize(
        user: UserId,
        legacy: (ClientId, RedirectUri) => Fu[Option[LegacyClientApi.HashedClientSecret]]
    )(using Executor): FuRaise[Error, Authorized] =
      val challengeFu = codeChallengeMethod.match
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
