package lila.user

import play.api.mvc.{ Request, RequestHeader }

sealed trait UserContext {

  val req: RequestHeader

  val me: Option[User]

  val sameOrigin: Boolean

  def isAuth = me.isDefined

  def isAnon = !isAuth

  def is(user: User): Boolean = me contains user

  def userId = me.map(_.id)

  def username = me.map(_.username)

  def usernameOrAnon = username | "Anonymous"

  def troll = me.??(_.troll)

  def ip = req.remoteAddress

  def kid = me.??(_.kid)
  def noKid = !kid
}

sealed abstract class BaseUserContext(val req: RequestHeader, val me: Option[User], val sameOrigin: Boolean) extends UserContext {

  override def toString = "%s %s %s".format(
    me.fold("Anonymous")(_.username),
    req.remoteAddress,
    req.headers.get("User-Agent") | "?"
  )
}

final class BodyUserContext[A](val body: Request[A], m: Option[User], so: Boolean)
  extends BaseUserContext(body, m, so)

final class HeaderUserContext(r: RequestHeader, m: Option[User], so: Boolean)
  extends BaseUserContext(r, m, so)

trait UserContextWrapper extends UserContext {
  val userContext: UserContext
  val req = userContext.req
  val me = userContext.me
  val sameOrigin = userContext.sameOrigin
}

object UserContext {

  def apply(req: RequestHeader, me: Option[User], sameOrigin: Boolean): HeaderUserContext =
    new HeaderUserContext(req, me, sameOrigin)

  def apply[A](req: Request[A], me: Option[User], sameOrigin: Boolean): BodyUserContext[A] =
    new BodyUserContext(req, me, sameOrigin)
}
