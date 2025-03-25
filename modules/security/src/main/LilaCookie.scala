package lila
package security

import play.api.mvc.*
import scalalib.SecureRandom

import lila.core.config.{ NetDomain, NetConfig }
import lila.core.security.LilaCookie

final class LilaCookie(baker: SessionCookieBaker, config: NetConfig) extends lila.core.security.LilaCookie:

  private val cookieDomain = config.domain.value.split(":").head

  def makeSessionId(using RequestHeader): Cookie = session(LilaCookie.sessionId, generateSessionId())

  def generateSessionId() = SecureRandom.nextString(22)

  def session(name: String, value: String, remember: Boolean = true)(using RequestHeader): Cookie =
    withSession(remember):
      _ + (name -> value)

  def newSession(using RequestHeader): Cookie =
    withSession(remember = false)(_ => Session.emptyCookie)

  def withSession(remember: Boolean)(op: Session => Session)(using req: RequestHeader): Cookie =
    cookie(
      baker.COOKIE_NAME,
      baker.encode(
        baker.serialize(
          op(
            (if remember then req.session - LilaCookie.noRemember
             else
               req.session + (LilaCookie.noRemember -> "1")
            ) + (LilaCookie.sessionId -> generateSessionId())
          )
        )
      ),
      if remember then none else 0.some
    )

  def cookie(name: String, value: String, maxAge: Option[Int] = None, httpOnly: Option[Boolean] = None)(using
      req: RequestHeader
  ): Cookie =
    Cookie(
      name,
      value,
      maxAge = if maxAge.has(0) then none else maxAge.orElse(baker.maxAge).orElse(86400.some),
      path = "/",
      domain = cookieDomain.some,
      secure = config.baseUrl.value.startsWith("https:"),
      httpOnly = httpOnly | baker.httpOnly,
      sameSite = Cookie.SameSite.Lax.some
    )

  def isRememberMe(req: RequestHeader) = !req.session.get(LilaCookie.noRemember).has("1")

  def discard(name: String) =
    DiscardingCookie(name, "/", cookieDomain.some, baker.httpOnly)

  def ensure(req: RequestHeader)(res: Result): Result =
    if req.session.data.contains(LilaCookie.sessionId) then res
    else res.withCookies(makeSessionId(using req))

  def ensureAndGet(req: RequestHeader)(res: String => Fu[Result])(using Executor): Fu[Result] =
    req.session.data.get(LilaCookie.sessionId) match
      case Some(sessionId) => res(sessionId)
      case None =>
        val sid = generateSessionId()
        res(sid).map:
          _.withCookies(session(LilaCookie.sessionId, sid)(using req))
