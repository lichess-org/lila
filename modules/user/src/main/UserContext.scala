package lila.user

import play.api.mvc.{ Request, RequestHeader }

sealed trait UserContext {

  val req: RequestHeader

  val me: Option[User]

  def isAuth = me.isDefined

  def isAnon = !isAuth

  def is(user: User): Boolean = me ?? (user ==)

  def userId = me map (_.id)

  def username = me map (_.username)

  def troll = me.??(_.troll)

  def ip = req.remoteAddress
}

sealed abstract class BaseUserContext(val req: RequestHeader, val me: Option[User]) extends UserContext {

  override def toString = "%s %s %s".format(
    me.fold("Anonymous")(_.username),
    req.remoteAddress,
    req.headers.get("User-Agent") | "?"
  )
}

final class BodyUserContext(val body: Request[_], m: Option[User])
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

  def apply(req: Request[_], me: Option[User]): BodyUserContext =
    new BodyUserContext(req, me)
}
