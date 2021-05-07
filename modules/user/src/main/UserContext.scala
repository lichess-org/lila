package lila.user

import play.api.mvc.{ Request, RequestHeader }
import play.api.i18n.Lang

sealed trait UserContext {

  val req: RequestHeader

  val me: Option[User]

  val impersonatedBy: Option[User]

  def lang: Lang

  def isAuth = me.isDefined

  def isAnon = !isAuth

  def is(user: User): Boolean = me contains user

  def userId = me.map(_.id)

  def username = me.map(_.username)

  def usernameOrAnon = username | "Anonymous"

  def troll = me.exists(_.marks.troll)

  def ip = lila.common.HTTPRequest ipAddress req

  def kid   = me.exists(_.kid)
  def noKid = !kid
}

sealed abstract class BaseUserContext(
    val req: RequestHeader,
    val me: Option[User],
    val impersonatedBy: Option[User],
    val lang: Lang
) extends UserContext {

  override def toString =
    "%s %s %s".format(
      me.fold("Anonymous")(_.username),
      req.remoteAddress,
      req.headers.get("User-Agent") | "?"
    )
}

final class BodyUserContext[A](val body: Request[A], m: Option[User], i: Option[User], l: Lang)
    extends BaseUserContext(body, m, i, l)

final class HeaderUserContext(r: RequestHeader, m: Option[User], i: Option[User], l: Lang)
    extends BaseUserContext(r, m, i, l)

trait UserContextWrapper extends UserContext {
  val userContext: UserContext
  val req            = userContext.req
  val me             = userContext.me
  val impersonatedBy = userContext.impersonatedBy
  def isBot          = me.exists(_.isBot)
  def noBot          = !isBot
  def isAppealUser   = me.exists(!_.enabled)
}

object UserContext {

  def apply(
      req: RequestHeader,
      me: Option[User],
      impersonatedBy: Option[User],
      lang: Lang
  ): HeaderUserContext =
    new HeaderUserContext(req, me, impersonatedBy, lang)

  def apply[A](
      req: Request[A],
      me: Option[User],
      impersonatedBy: Option[User],
      lang: Lang
  ): BodyUserContext[A] =
    new BodyUserContext(req, me, impersonatedBy, lang)

  trait ToLang {
    implicit def ctxLang(implicit ctx: UserContext): Lang = ctx.lang
  }
}
