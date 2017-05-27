package lila.user

import play.api.i18n.Lang
import play.api.mvc.{ Request, RequestHeader }

sealed trait UserContext {

  val req: RequestHeader

  val me: Option[User]

  def lang: Lang

  def isAuth = me.isDefined

  def isAnon = !isAuth

  def is(user: User): Boolean = me contains user

  def userId = me.map(_.id)

  def username = me.map(_.username)

  def usernameOrAnon = username | "Anonymous"

  def troll = me.??(_.troll)

  def ip = lila.common.HTTPRequest lastRemoteAddress req

  def kid = me.??(_.kid)
  def noKid = !kid
}

sealed abstract class BaseUserContext(
    val req: RequestHeader,
    val me: Option[User],
    val lang: Lang
) extends UserContext {

  override def toString = "%s %s %s".format(
    me.fold("Anonymous")(_.username),
    req.remoteAddress,
    req.headers.get("User-Agent") | "?"
  )
}

final class BodyUserContext[A](val body: Request[A], m: Option[User], l: Lang)
  extends BaseUserContext(body, m, l)

final class HeaderUserContext(r: RequestHeader, m: Option[User], l: Lang)
  extends BaseUserContext(r, m, l)

trait UserContextWrapper extends UserContext {
  val userContext: UserContext
  val req = userContext.req
  val me = userContext.me
}

object UserContext {

  def apply(req: RequestHeader, me: Option[User], lang: Lang): HeaderUserContext =
    new HeaderUserContext(req, me, lang)

  def apply[A](req: Request[A], me: Option[User], lang: Lang): BodyUserContext[A] =
    new BodyUserContext(req, me, lang)
}
