package lila.oidc

/** Why an existing lila account must not receive a session after SSO authentication. */
enum LoginRefusal:
  case Closed, ClosedForever, Deleted

object LoginRefusal:

  /** Deleted and forever-closed take precedence over a temporary closure.
    * `enabled = true` always wins: the account is allowed to log in.
    */
  def of(enabled: Boolean, foreverClosed: Boolean, deleted: Boolean): Option[LoginRefusal] =
    if enabled then None
    else if deleted then Some(LoginRefusal.Deleted)
    else if foreverClosed then Some(LoginRefusal.ClosedForever)
    else Some(LoginRefusal.Closed)
