package lila.common

import ornicar.scalalib.Random
import play.api.mvc._
import scala.concurrent.ExecutionContext

import lila.common.config.NetDomain

final class LilaCookie(domain: NetDomain, baker: SessionCookieBaker) {

  private val cookieDomain = domain.value.split(":").head

  def makeSessionId(implicit req: RequestHeader) = session(LilaCookie.sessionId, generateSessionId())

  def generateSessionId() = Random secureString 22

  def session(name: String, value: String)(implicit req: RequestHeader): Cookie =
    withSession { s =>
      s + (name -> value)
    }

  def newSession(implicit req: RequestHeader): Cookie = withSession(_ => Session.emptyCookie)

  def withSession(op: Session => Session)(implicit req: RequestHeader): Cookie =
    cookie(
      baker.COOKIE_NAME,
      baker.encode(baker.serialize(op(req.session)))
    )

  def cookie(name: String, value: String, maxAge: Option[Int] = None, httpOnly: Option[Boolean] = None)(
      implicit req: RequestHeader
  ): Cookie =
    Cookie(
      name,
      value,
      maxAge orElse baker.maxAge orElse 86400.some,
      "/",
      cookieDomain.some,
      baker.secure || req.headers.get("X-Forwarded-Proto").contains("https"),
      httpOnly | baker.httpOnly
    )

  def discard(name: String) =
    DiscardingCookie(name, "/", cookieDomain.some, baker.httpOnly)

  def ensure(req: RequestHeader)(res: Result): Result =
    if (req.session.data.contains(LilaCookie.sessionId)) res
    else res withCookies makeSessionId(req)

  def ensureAndGet(req: RequestHeader)(res: String => Fu[Result])(implicit ec: ExecutionContext): Fu[Result] =
    req.session.data.get(LilaCookie.sessionId) match {
      case Some(sessionId) => res(sessionId)
      case None =>
        val sid = generateSessionId()
        res(sid) map {
          _ withCookies session(LilaCookie.sessionId, sid)(req)
        }
    }
}

object LilaCookie {

  val sessionId = "sid"
}
