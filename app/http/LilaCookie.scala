package lila
package http

import play.api.mvc.{ Cookie, Session, RequestHeader }

object LilaCookie {

  private val domainRegex = """^.+(\.[^\.]+\.[^\.]+)$""".r

  private def domain(req: RequestHeader): String =
    domainRegex.replaceAllIn(req.domain, _ group 1)

  def apply(name: String, value: String)(implicit req: RequestHeader): Cookie = {
    val data = req.session + (name -> value)
    val encoded = Session.encode(Session.serialize(data))
    Cookie(
      Session.COOKIE_NAME, 
      encoded, 
      Session.maxAge, 
      "/", 
      domain(req).some, 
      Session.secure, 
      Session.httpOnly)
  }
}
