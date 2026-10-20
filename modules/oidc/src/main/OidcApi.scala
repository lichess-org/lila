package lila.oidc

import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.openid.connect.sdk.Nonce
import com.nimbusds.openid.connect.sdk.claims.UserInfo
import lila.common.EmailAddress
import play.api.mvc.RequestHeader

final class OidcApi(
    oidc: OIDC,
    provisioning: UserProvisioning,
    stateRepo: OidcStateRepo
)(using Executor):

  private val client = new OpenIDConnectService(oidc)

  def getAuthenticationRequest(): Fu[(AuthenticationRequest, com.nimbusds.oauth2.sdk.pkce.CodeVerifier)] =
    client.createOIDCAuthenticationRequest()

  def saveLoginState(
      request: AuthenticationRequest,
      referrer: Option[String],
      codeVerifier: com.nimbusds.oauth2.sdk.pkce.CodeVerifier
  ): Funit =
    stateRepo.put(
      request.getState.getValue,
      request.getNonce.getValue,
      codeVerifier.getValue,
      referrer
    )

  def authenticate(req: RequestHeader): Fu[LoginOutcome] =
    val state       = req.getQueryString("state").getOrElse("")
    val cookieState = req.cookies.get("oidc_state").map(_.value)

    if state.isEmpty || !cookieState.contains(state) then
      fuccess(LoginOutcome.Refused(LoginError.InvalidState, None))
    else
      stateRepo.consume(state).flatMap {
        case None => fuccess(LoginOutcome.Refused(LoginError.InvalidState, None))
        case Some(loginState) =>
          client
            .authenticate(
              req.queryString,
              new State(state),
              new Nonce(loginState.nonce),
              new com.nimbusds.oauth2.sdk.pkce.CodeVerifier(loginState.codeVerifier)
            )
            .flatMap { authn =>
              externalIdentity(authn.userInfo) match
                case Left(error) => fuccess(LoginOutcome.Refused(error, None))
                case Right(identity) =>
                  provisioning.resolve(identity).map {
                    case LoginOutcome.Success(user, _) => LoginOutcome.Success(user, authn.session.some)
                    case other                         => other
                  }
            }
            .recover { case _ =>
              LoginOutcome.Refused(LoginError.ProviderError, None)
            }
      }

  def issuer: String = oidc.issuer.getValue

  def endSessionUrl(idToken: String): Fu[Option[String]] =
    client.endSessionUrl(idToken)

  def validateLogoutToken(token: String): Fu[com.nimbusds.openid.connect.sdk.claims.LogoutTokenClaimsSet] =
    client.validateLogoutToken(token)

  private def externalIdentity(userInfo: UserInfo): Either[LoginError, ExternalIdentity] =
    for
      subject <- Option(userInfo.getSubject)
        .map(_.getValue)
        .filter(_.nonEmpty)
        .toRight(LoginError.ClaimsError)
      username <- Option(userInfo.getPreferredUsername)
        .map(_.trim)
        .filter(_.nonEmpty)
        .toRight(LoginError.ClaimsError)
      email         = Option(userInfo.getEmailAddress).flatMap(EmailAddress.from)
      emailVerified = Option(userInfo.getEmailVerified).exists(_.booleanValue)
      globallyEnabled = Option(userInfo.getClaim("globally_enabled")).flatMap:
        case b: java.lang.Boolean => Some(b.booleanValue)
        case _                    => None
    yield ExternalIdentity(
      issuer = oidc.issuer.getValue,
      subject = subject,
      username = username,
      email = email,
      emailVerified = emailVerified,
      globallyEnabled = globallyEnabled
    )
