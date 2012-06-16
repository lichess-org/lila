package lila
package http

import play.api.mvc.{ Cookie, Session, RequestHeader }

object LilaCookie {

  private val domainRegex = """^.+(\.[^\.]+\.[^\.]+)$""".r

  private def domain(req: RequestHeader): String =
    domainRegex.replaceAllIn(req.domain, _ group 1)

  def session(name: String, value: String)(implicit req: RequestHeader): Cookie = cookie(
    Session.COOKIE_NAME,
    Session.encode(Session.serialize(req.session + (name -> value))))

  def cookie(name: String, value: String, maxAge: Option[Int] = None)(implicit req: RequestHeader): Cookie = Cookie(
    name,
    value,
    maxAge | Session.maxAge,
    "/",
    domain(req).some,
    Session.secure,
    Session.httpOnly)
}
