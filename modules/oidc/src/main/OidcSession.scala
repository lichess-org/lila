package lila.oidc

/** Session material needed for RP-initiated and backchannel logout. */
case class OidcSession(
    idToken: String,
    issuer: String,
    sid: Option[String],
    authTime: Option[Instant]
)

case class OidcAuthn(
    userInfo: com.nimbusds.openid.connect.sdk.claims.UserInfo,
    session: OidcSession
)
