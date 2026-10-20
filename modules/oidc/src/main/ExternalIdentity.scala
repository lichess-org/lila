package lila.oidc

import lila.common.EmailAddress

/** Provider-agnostic identity as delivered by the IdP. `subject` is the canonical, immutable `sub` and is
  * used as the lila `UserId`.
  */
case class ExternalIdentity(
    issuer: String,
    subject: String,
    username: String,
    email: Option[EmailAddress],
    emailVerified: Boolean,
    globallyEnabled: Option[Boolean]
)
