package lila.oidc

import scalalib.SecureRandom
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.openid.connect.sdk.Nonce
import com.nimbusds.openid.connect.sdk.claims.UserInfo
import lila.common.EmailAddress
import lila.core.security.ClearPassword
import lila.user.{ User, UserRepo }
import play.api.mvc.RequestHeader

final class OidcApi(
    oidc: OIDC,
    userRepo: lila.user.UserRepo,
    authenticator: lila.core.security.Authenticator
)(using Executor, akka.stream.Materializer):

  def getAuthenticationRequest(): Fu[AuthenticationRequest] =
    val client                = new OpenIDConnectService(oidc);
    val authenticationRequest = client.createOIDCAuthenticationRequest()
    authenticationRequest

  def authenticate(req: RequestHeader): Fu[Option[User]] =
    // val redirectBackURI = req.flash("referrer")
    val state  = req.flash.get("oidcState").getOrElse("~")
    val nonce  = req.flash.get("oidcNonce").getOrElse("~")
    val params = req.queryString
    val client = new OpenIDConnectService(oidc);

    client.authenticate(params, new State(state), new Nonce(nonce)).flatMap { userInfo =>
      getOrCreateUser(userInfo)
    }

  def getOrCreateUser(userInfo: UserInfo): Fu[Option[User]] =
    userRepo.byId(UserId(userInfo.getSubject.toString)).flatMap {
      case Some(user) =>
        user.enabled.yes.so(userRepo.setUsername(user.id, userInfo.getPreferredUsername))
        fuccess(Some(user))
      case None =>
        createOidcUser(userInfo)
    }

  def createOidcUser(userInfo: UserInfo): Fu[Option[User]] =
    val pwd          = authenticator.passEnc(ClearPassword(SecureRandom.nextString(12)))
    val blind        = false
    val confirmEmail = false
    val email        = EmailAddress(userInfo.getEmailAddress)
    val username     = userInfo.getPreferredUsername
    userRepo.create2(UserId(userInfo.getSubject.toString), username, pwd, email, blind, none, confirmEmail)
