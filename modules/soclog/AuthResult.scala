package lila.soclog

import play.api.mvc.Result

/**
 * An object that represents the different results of the authentication flow
 */
sealed trait AuthResult

object AuthResult {
  /**
   * A user denied access to their account while authenticating with an external provider (eg: Twitter)
   */
  case object AccessDenied extends AuthResult

  /**
   * An intermetiate result during the authentication flow (maybe a redirection to the external provider page)
   */
  case class NavigationFlow(result: Result) extends AuthResult

  /**
   * Returned when the user was succesfully authenticated
   * @param profile the authenticated user profile
   */
  case class Authenticated(profile: Profile) extends AuthResult

  /**
   * Returned when the authentication process failed for some reason.
   * @param error a description of the error
   */
  case class Failed(error: String) extends AuthResult
}
