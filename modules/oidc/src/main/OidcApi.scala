package lila.oidc

import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.openid.connect.sdk.Nonce
import com.nimbusds.openid.connect.sdk.claims.UserInfo
import lila.common.EmailAddress
import play.api.mvc.RequestHeader

final class OidcApi(
    oidc: OIDC,
    provisioning: UserProvisioning
)(using Executor):

  private val client = new OpenIDConnectService(oidc)

  def getAuthenticationRequest(): Fu[AuthenticationRequest] =
    client.createOIDCAuthenticationRequest()

  def authenticate(req: RequestHeader): Fu[LoginOutcome] =
    val state  = req.flash.get("oidcState").getOrElse("~")
    val nonce  = req.flash.get("oidcNonce").getOrElse("~")
    val params = req.queryString

    client
      .authenticate(params, new State(state), new Nonce(nonce))
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

  def endSessionUrl(idToken: String, postLogoutRedirect: String): Fu[Option[String]] =
    client.endSessionUrl(idToken, postLogoutRedirect)

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
      email           = Option(userInfo.getEmailAddress).flatMap(EmailAddress.from)
      emailVerified   = Option(userInfo.getEmailVerified).exists(_.booleanValue)
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
