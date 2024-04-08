package lila.core
package security

import lila.core.user.UserEnabled

trait LilaCookie:
  import play.api.mvc.*
  def cookie(name: String, value: String, maxAge: Option[Int] = None, httpOnly: Option[Boolean] = None)(using
      RequestHeader
  ): Cookie
