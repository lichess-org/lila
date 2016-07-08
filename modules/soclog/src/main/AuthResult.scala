package lila.soclog

import lila.user.User

sealed trait AuthResult

object AuthResult {

  case object Nope extends AuthResult

  case class Authenticated(user: User) extends AuthResult

  case class PickUsername(oAuth: OAuth) extends AuthResult
}
