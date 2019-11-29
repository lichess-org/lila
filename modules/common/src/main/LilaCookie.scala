package lila.common

import ornicar.scalalib.Random
import play.api.mvc.{ Cookie, DiscardingCookie, Session, RequestHeader, SessionCookieBaker }

final class LilaCookie(baker: SessionCookieBaker) {

  private val domainRegex = """\.[^.]++\.[^.]++$""".r

  private def domain(req: RequestHeader): String =
    domainRegex.findFirstIn(req.domain).getOrElse(req.domain)

  def makeSessionId(implicit req: RequestHeader) = session(LilaCookie.sessionId, Random secureString 22)

  def session(name: String, value: String)(implicit req: RequestHeader): Cookie = withSession { s =>
    s + (name -> value)
  }

  def newSession(implicit req: RequestHeader): Cookie = withSession(_ => Session.emptyCookie)

  def withSession(op: Session => Session)(implicit req: RequestHeader): Cookie = cookie(
    baker.COOKIE_NAME,
    baker.encode(baker.serialize(op(req.session)))
  )

  def cookie(name: String, value: String, maxAge: Option[Int] = None, httpOnly: Option[Boolean] = None)(implicit req: RequestHeader): Cookie = Cookie(
    name,
    value,
    maxAge orElse baker.maxAge orElse 86400.some,
    "/",
    domain(req).some,
    baker.secure || req.headers.get("X-Forwarded-Proto").contains("https"),
    httpOnly | baker.httpOnly
  )

  def discard(name: String)(implicit req: RequestHeader) =
    DiscardingCookie(name, "/", domain(req).some, baker.httpOnly)
}

object LilaCookie {

  val sessionId = "sid"
}
