package lila.oidc

import java.io.IOException

import com.nimbusds.jose.proc.BadJOSEException
import com.nimbusds.jose.util.DefaultResourceRetriever
import com.nimbusds.jose.JOSEException
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.*
import com.nimbusds.oauth2.sdk.auth.{ ClientSecretBasic, ClientSecretPost }
import com.nimbusds.oauth2.sdk.http.HTTPResponse
import com.nimbusds.oauth2.sdk.id.{ Issuer, State }
import com.nimbusds.oauth2.sdk.pkce.{ CodeChallengeMethod, CodeVerifier }
import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet
import com.nimbusds.openid.connect.sdk.claims.LogoutTokenClaimsSet
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata
import com.nimbusds.openid.connect.sdk.token.OIDCTokens
import com.nimbusds.openid.connect.sdk.validators.{ IDTokenValidator, LogoutTokenValidator }
import com.nimbusds.openid.connect.sdk.{ AuthenticationErrorResponse, * }
import com.nimbusds.openid.connect.sdk.claims.*

import scala.jdk.CollectionConverters.*

// http://nemcio.cf/gitbucket/

final class OpenIDConnectService(
    val oidc: OIDC
)(implicit ec: scala.concurrent.ExecutionContext):

  private lazy val metadata = getMetadata(oidc.issuer)

  private val JWK_REQUEST_TIMEOUT = 5000

  private val OIDC_SCOPE = new Scope(oidc.scopes.toSeq*)

  /** Create an authentication request.
    *
    * @return
    *   Authentication request
    */
  def createOIDCAuthenticationRequest(): Fu[(AuthenticationRequest, CodeVerifier)] = metadata.map {
    metadata =>
      val codeVerifier = new CodeVerifier()
      val request = new AuthenticationRequest.Builder(
        new ResponseType(ResponseType.Value.CODE),
        OIDC_SCOPE,
        oidc.clientID,
        oidc.signinCallbackUrl
      )
        .endpointURI(metadata.getAuthorizationEndpointURI)
        .state(new State())
        .nonce(new Nonce())
        .codeChallenge(codeVerifier, CodeChallengeMethod.S256)
        .build()
      (request, codeVerifier)
  }

  /** Proceed the OpenID Connect authentication.
    *
    * @param params
    *   Query parameters of the authentication response
    * @param state
    *   State saved in the session
    * @param nonce
    *   Nonce saved in the session
    * @return
    *   User
    */
  def authenticate(
      params: Map[String, Seq[String]],
      state: State,
      nonce: Nonce,
      codeVerifier: CodeVerifier
  ): Fu[OidcAuthn] =
    validateOIDCAuthenticationResponse(params, state).flatMap { authenticationResponse =>
      obtainOIDCTokens(authenticationResponse.getAuthorizationCode, nonce, codeVerifier).flatMap {
        (tokens, claims) =>
          obtainUserInfo(tokens.getBearerAccessToken).map { userInfo =>
            OidcAuthn(
              userInfo = userInfo,
              session = OidcSession(
                idToken = tokens.getIDToken.serialize,
                issuer = oidc.issuer.getValue,
                sid = Option(claims.getSessionID).map(_.getValue),
                authTime = Option(claims.getAuthenticationTime).map(_.toInstant)
              )
            )
          }
      }
    }

  /** RP-initiated logout URL, if the provider advertises an end_session_endpoint. */
  /** RP-initiated logout URL. `post_logout_redirect_uri` is omitted because the IdP client does not register
    * it; IdentityServer then ignores the request and keeps the SSO session alive. With `id_token_hint` +
    * `state` it redirects to `/signout-oidc`.
    */
  def endSessionUrl(idToken: String): Fu[Option[String]] =
    metadata.map: metadata =>
      Option(metadata.getEndSessionEndpointURI).map: endpoint =>
        val separator = if endpoint.toString.contains("?") then "&" else "?"
        val encode =
          (value: String) => java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8)
        val state = new State()
        s"$endpoint${separator}id_token_hint=${encode(idToken)}&state=${encode(state.getValue)}"

  /** Validate a signed backchannel logout token. */
  def validateLogoutToken(token: String): Fu[LogoutTokenClaimsSet] =
    metadata.flatMap { metadata =>
      JwsAlgorithmValidation.ensureSupported(oidc.jwsAlgorithm)(metadata) match
        case Left(err) => fufail(err)
        case Right(jwsAlgorithm) =>
          val validator = new LogoutTokenValidator(
            metadata.getIssuer,
            oidc.clientID,
            jwsAlgorithm,
            metadata.getJWKSetURI.toURL,
            new DefaultResourceRetriever(JWK_REQUEST_TIMEOUT, JWK_REQUEST_TIMEOUT)
          )
          try fuccess(validator.validate(SignedJWT.parse(token)))
          catch
            case e @ (_: BadJOSEException | _: JOSEException | _: java.text.ParseException) =>
              fufail(s"OIDC logout token has error: $e")
    }

  private def converParams(params: Map[String, Seq[String]]) =
    params.map { case (k, v) => (k, v.toList.asJava) }.asJava

  /** Validate the authentication response.
    *
    * @param params
    *   Query parameters of the authentication response
    * @param state
    *   State saved in the session
    * @return
    *   Authentication response
    */
  def validateOIDCAuthenticationResponse(
      params: Map[String, Seq[String]],
      state: State
  ): Fu[AuthenticationSuccessResponse] =
    try
      AuthenticationResponseParser.parse(oidc.signinCallbackUrl, converParams(params)) match
        case response: AuthenticationSuccessResponse =>
          if response.getState == state then fuccess(response)
          else
            fufail(
              s"OIDC authentication state did not match: response(${response.getState}) != session($state)"
            )
        case response: AuthenticationErrorResponse =>
          fufail(s"OIDC authentication response has error: ${response.getErrorObject}")
    catch
      case e: ParseException =>
        fufail(s"OIDC authentication response has error: $e")

  /** Obtain the ID token from the OpenID Provider.
    *
    * @param authorizationCode
    *   Authorization code in the query string
    * @param nonce
    *   Nonce
    * @return
    *   Token response
    */
  def obtainOIDCTokens(
      authorizationCode: AuthorizationCode,
      nonce: Nonce,
      codeVerifier: CodeVerifier
  ): Fu[(OIDCTokens, IDTokenClaimsSet)] =
    sendOIDCTokenRequest(authorizationCode, codeVerifier).flatMap { httpResponse =>
      try
        OIDCTokenResponseParser.parse(httpResponse) match
          case response: OIDCTokenResponse =>
            validateOIDCTokenResponse(response, nonce).map { claims =>
              (response.getOIDCTokens, claims)
            }
          case response: TokenErrorResponse =>
            fufail(s"OIDC token response has error: ${response.getErrorObject.toJSONObject}")
      catch
        case e: ParseException =>
          fufail(s"OIDC token response has error: $e")
    }

  /** Execute oidc token request
    * @param authorizationCode
    *   OpentId authorization code
    * @return
    */
  private def sendOIDCTokenRequest(
      authorizationCode: AuthorizationCode,
      codeVerifier: CodeVerifier
  ): Fu[HTTPResponse] = buildOIDCTokenRequest(authorizationCode, codeVerifier).flatMap { request =>
    try fuccess(request.toHTTPRequest.send())
    catch
      case se: SerializeException =>
        fufail(s"Failed to send oidc code verification request (SerializeException) $se")

      case e: IOException =>
        fufail(s"Failed to send oidc code verification request (IOException) $e")
  }

  /** Create oidc token request
    * @param authorizationCode
    *   OpentId authorization code
    * @return
    */
  private def buildOIDCTokenRequest(
      authorizationCode: AuthorizationCode,
      codeVerifier: CodeVerifier
  ): Fu[TokenRequest] = metadata.flatMap { metadata =>
    val clientAuth = oidc.tokenAuthMethod match
      case "client_secret_post"  => new ClientSecretPost(oidc.clientID, oidc.clientSecret)
      case "client_secret_basic" => new ClientSecretBasic(oidc.clientID, oidc.clientSecret)
      case other => throw new IllegalArgumentException(s"Unsupported token auth method: $other")
    fuccess(
      new TokenRequest(
        metadata.getTokenEndpointURI,
        clientAuth,
        new AuthorizationCodeGrant(authorizationCode, oidc.signinCallbackUrl, codeVerifier),
        OIDC_SCOPE
      )
    )
  }

  /** Validate the token response.
    *
    * @param response
    *   Token response
    * @param nonce
    *   Nonce
    * @return
    *   Claims
    */
  private def validateOIDCTokenResponse(
      response: OIDCTokenResponse,
      nonce: Nonce
  ): Fu[IDTokenClaimsSet] = Option(response.getOIDCTokens.getIDToken) match
    case Some(jwt) =>
      metadata.flatMap { metadata =>
        JwsAlgorithmValidation.ensureSupported(oidc.jwsAlgorithm)(metadata) match
          case Left(err) => fufail(err)
          case Right(jwsAlgorithm) =>
            val validator = new IDTokenValidator(
              metadata.getIssuer,
              oidc.clientID,
              jwsAlgorithm,
              metadata.getJWKSetURI.toURL,
              new DefaultResourceRetriever(JWK_REQUEST_TIMEOUT, JWK_REQUEST_TIMEOUT)
            )
            try fuccess(validator.validate(jwt, nonce))
            catch
              case e @ (_: BadJOSEException | _: JOSEException) =>
                fufail(s"OIDC ID token has error: $e")
      }
    case None =>
      fufail(s"OIDC token response does not have a valid ID token: ${response.toJSONObject}")

  /** Retrieve user info from oidc provider
    * @param token
    * @return
    */
  def obtainUserInfo(
      token: BearerAccessToken
  ): Fu[UserInfo] = sendUserInfoRequest(token).flatMap { httpResponse =>
    try
      UserInfoResponse.parse(httpResponse) match
        case successResponse: UserInfoSuccessResponse =>
          fuccess(successResponse.getUserInfo)
        case errorResponse: UserInfoErrorResponse =>
          fufail(s"OIDC token response has error: ${errorResponse.getErrorObject.toJSONObject}")
    catch
      case e: ParseException =>
        fufail(s"UserInfo response has error: $e")
  }

  private def sendUserInfoRequest(
      token: BearerAccessToken
  ): Fu[HTTPResponse] = buildUserInfoRequest(token).flatMap { request =>
    try fuccess(request.toHTTPRequest.send())
    catch
      case se: SerializeException =>
        fufail(s"Failed to send oidc user info request (SerializeException) $se")
      case e: IOException =>
        fufail(s"Failed to send oidc user info request (IOException) $e")
  }

  private def buildUserInfoRequest(
      token: BearerAccessToken
  ): Fu[UserInfoRequest] = metadata.flatMap { metadata =>
    fuccess(
      new UserInfoRequest(metadata.getUserInfoEndpointURI, token)
    )
  }

  /** Retrieve oidc provider metadata
    *
    * @param issuer
    * @return
    *   Either[String, OIDCProviderMetadata]
    */
  private def getMetadata(issuer: Issuer): Fu[OIDCProviderMetadata] =
    try fuccess(OIDCProviderMetadata.resolve(issuer))
    catch
      case e @ (_: GeneralException | _: IOException) =>
        fufail(s"Unable retrieve OIDC metadata")
