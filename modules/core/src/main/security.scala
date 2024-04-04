package lila.core
package security

import play.api.mvc.*

trait LilaCookie:
  def cookie(name: String, value: String, maxAge: Option[Int] = None, httpOnly: Option[Boolean] = None)(using
      RequestHeader
  ): Cookie
