package lila.oidc

import lila.user.User

/** Typed failures of the SSO login flow. */
enum LoginError:
  case InvalidState, ProviderError, TokenError, ClaimsError
  case Banned, Closed, ClosedForever, Deleted

object LoginError:

  /** Decide why an existing account must not receive a session. A global ban always wins; then local
    * deletion/closure status applies.
    */
  def fromStatus(
      enabled: Boolean,
      foreverClosed: Boolean,
      deleted: Boolean,
      globallyEnabled: Option[Boolean]
  ): Option[LoginError] =
    if globallyEnabled.contains(false) then Some(LoginError.Banned)
    else if enabled then None
    else if deleted then Some(LoginError.Deleted)
    else if foreverClosed then Some(LoginError.ClosedForever)
    else Some(LoginError.Closed)

/** Result of mapping an external identity to a lila account. `user` is present for status refusals, so the
  * caller can start an appeal session.
  */
enum LoginOutcome:
  case Success(user: User, oidcSession: Option[OidcSession])
  case Refused(error: LoginError, user: Option[User])
