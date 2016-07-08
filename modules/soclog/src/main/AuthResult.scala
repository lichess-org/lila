package lila.soclog

import lila.user.User

sealed trait AuthResult

object AuthResult {

  case object BadRequest extends AuthResult

  case object AccessDenied extends AuthResult

  case class Authenticated(user: User) extends AuthResult
}

sealed trait SignUpResult extends AuthResult

object SignUpResult {

  case object Failed extends SignUpResult

  case class SignedUp(user: User) extends SignUpResult

  case class ExistingUsername(oauth: OAuth, existingUser: User) extends SignUpResult
}
