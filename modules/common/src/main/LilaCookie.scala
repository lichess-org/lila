package lila.common

import java.util.regex.Matcher.quoteReplacement

import ornicar.scalalib.Random
import play.api.mvc.{ Cookie, Session, RequestHeader }

object LilaCookie {

  private val domainRegex = """^.+(\.[^\.]+\.[^\.]+)$""".r

  private def domain(req: RequestHeader): String =
    domainRegex.replaceAllIn(req.domain, m => quoteReplacement(m group 1))

  val sessionId = "sid"

  def makeSessionId(implicit req: RequestHeader) = session(sessionId, Random nextStringUppercase 8)

  def session(name: String, value: String)(implicit req: RequestHeader): Cookie = withSession { s =>
    s + (name -> value)
  }

  def newSession(implicit req: RequestHeader): Cookie = withSession(identity)

  def withSession(op: Session => Session)(implicit req: RequestHeader): Cookie = cookie(
    Session.COOKIE_NAME,
    Session.encode(Session.serialize(op(req.session)))
  )

  def cookie(name: String, value: String, maxAge: Option[Int] = None, httpOnly: Option[Boolean] = None)(implicit req: RequestHeader): Cookie = Cookie(
    name,
    value,
    maxAge orElse Session.maxAge orElse 86400.some,
    "/",
    domain(req).some,
    Session.secure || req.headers.get("X-Forwarded-Proto").contains("https"),
    httpOnly | Session.httpOnly)
}
