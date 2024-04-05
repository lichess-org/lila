package lila.core
package security

import lila.core.user.UserEnabled

trait LilaCookie:
  import play.api.mvc.*
  def cookie(name: String, value: String, maxAge: Option[Int] = None, httpOnly: Option[Boolean] = None)(using
      RequestHeader
  ): Cookie

trait Grantable:
  def enabled: UserEnabled
  def roles: List[String]
object Grantable:
  given (using u: user.User): Grantable = new Grantable:
    def enabled = u.enabled
    def roles   = u.roles
