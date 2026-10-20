package lila.oidc

import java.net.URI

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.oauth2.sdk.auth.Secret
import com.nimbusds.oauth2.sdk.id.{ ClientID, Issuer }

case class OIDC(
    issuer: Issuer,
    clientID: ClientID,
    clientSecret: Secret,
    signinCallbackUrl: URI,
    jwsAlgorithm: JWSAlgorithm,
    tokenAuthMethod: String
)

object OIDC:
  val tokenAuthMethods = Set("client_secret_post", "client_secret_basic")

object OIDCFactory:
  def apply(
      issuer: String,
      clientID: String,
      clientSecret: String,
      signinCallbackUrl: String,
      jwsAlgorithm: String,
      tokenAuthMethod: String
  ): OIDC =
    if !OIDC.tokenAuthMethods.contains(tokenAuthMethod) then
      throw new IllegalArgumentException(
        s"oidc.issuer.token_auth_method '$tokenAuthMethod' is not supported"
      )
    new OIDC(
      new Issuer(issuer),
      new ClientID(clientID),
      new Secret(clientSecret),
      new URI(signinCallbackUrl),
      JwsAlgorithmValidation
        .parse(jwsAlgorithm)
        .fold(err => throw new IllegalArgumentException(err), identity),
      tokenAuthMethod
    )
