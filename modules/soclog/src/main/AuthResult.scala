package lila.soclog

import lila.user.User

sealed trait AuthResult

object AuthResult {

  case object BadRequest extends AuthResult

  case object AccessDenied extends AuthResult

  case class Authenticated(user: User) extends AuthResult

  case object Failed extends AuthResult

  case class PickUsername(oAuth: OAuth) extends AuthResult
}
