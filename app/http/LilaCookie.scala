package lila
package http

import play.api.mvc.{ Cookie, Session, RequestHeader }

object LilaCookie {

  import Session._

  private val domainRegex = """^.+(\.[^\.]+\.[^\.]+)$""".r

  private def domain(req: RequestHeader): String =
    domainRegex.replaceAllIn(req.domain, _ group 1)

  def apply(name: String, value: String)(implicit req: RequestHeader): Cookie = {
    val data = req.session + (name -> value)
    val encoded = encode(serialize(data))
    Cookie(COOKIE_NAME, encoded, maxAge, "/", domain(req).some, secure, httpOnly)
  }
}
