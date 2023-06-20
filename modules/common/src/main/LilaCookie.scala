package lila.common

import play.api.mvc.*
import ornicar.scalalib.SecureRandom

import lila.common.config.NetDomain

final class LilaCookie(domain: NetDomain, baker: SessionCookieBaker):

  private val cookieDomain = domain.value.split(":").head

  val makeSessionId = (req: RequestHeader) ?=> session(LilaCookie.sessionId, generateSessionId())

  def generateSessionId() = SecureRandom nextString 22

  def session(name: String, value: String, remember: Boolean = true)(using RequestHeader): Cookie =
    withSession(remember) { s =>
      s + (name -> value)
    }

  def newSession(using req: RequestHeader): Cookie =
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
      if (remember) none else 0.some
    )

  def cookie(name: String, value: String, maxAge: Option[Int] = None, httpOnly: Option[Boolean] = None)(using
      req: RequestHeader
  ): Cookie =
    Cookie(
      name,
      value,
      if (maxAge has 0) none
      else maxAge orElse baker.maxAge orElse 86400.some,
      "/",
      cookieDomain.some,
      baker.secure || req.headers.get("X-Forwarded-Proto").contains("https"),
      httpOnly | baker.httpOnly
    )

  def isRememberMe(req: RequestHeader) = !req.session.get(LilaCookie.noRemember).has("1")

  def discard(name: String) =
    DiscardingCookie(name, "/", cookieDomain.some, baker.httpOnly)

  def ensure(req: RequestHeader)(res: Result): Result =
    if (req.session.data.contains(LilaCookie.sessionId)) res
    else res withCookies makeSessionId(using req)

  def ensureAndGet(req: RequestHeader)(res: String => Fu[Result])(using Executor): Fu[Result] =
    req.session.data.get(LilaCookie.sessionId) match
      case Some(sessionId) => res(sessionId)
      case None =>
        val sid = generateSessionId()
        res(sid) map {
          _ withCookies session(LilaCookie.sessionId, sid)(using req)
        }

object LilaCookie:

  val sessionId  = "sid"
  val noRemember = "noRemember"
