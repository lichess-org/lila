package lila.core
package security

import lila.core.user.UserEnabled

trait LilaCookie:
  import play.api.mvc.*
  def cookie(name: String, value: String, maxAge: Option[Int] = None, httpOnly: Option[Boolean] = None)(using
      RequestHeader
  ): Cookie

opaque type FloodSource = String
object FloodSource extends OpaqueString[FloodSource]
trait FloodApi:
  def allowMessage(source: FloodSource, text: String): Boolean

trait SpamApi:
  def detect(text: String): Boolean
  def replace(text: String): String
