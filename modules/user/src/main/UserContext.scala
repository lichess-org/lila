package lila.user

import play.api.mvc.{ Request, RequestHeader }

sealed trait UserContext {

  val req: RequestHeader

  val me: Option[User]

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

sealed abstract class BaseUserContext(val req: RequestHeader, val me: Option[User]) extends UserContext {

  override def toString = "%s %s %s".format(
    me.fold("Anonymous")(_.username),
    req.remoteAddress,
    req.headers.get("User-Agent") | "?"
  )
}

final class BodyUserContext[A](val body: Request[A], m: Option[User])
  extends BaseUserContext(body, m)

final class HeaderUserContext(r: RequestHeader, m: Option[User])
  extends BaseUserContext(r, m)

trait UserContextWrapper extends UserContext {
  val userContext: UserContext
  val req = userContext.req
  val me = userContext.me
}

object UserContext {

  def apply(req: RequestHeader, me: Option[User]): HeaderUserContext =
    new HeaderUserContext(req, me)

  def apply[A](req: Request[A], me: Option[User]): BodyUserContext[A] =
    new BodyUserContext(req, me)
}
