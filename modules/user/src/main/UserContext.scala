package lila.user

import play.api.mvc.{ Request, RequestHeader }
import play.api.i18n.Lang
import lila.common.HTTPRequest

class UserContext(
    val req: RequestHeader,
    val me: Option[User],
    val impersonatedBy: Option[User],
    val lang: Lang
):
  export me.{ isDefined as isAuth, isEmpty as isAnon }

  def is[U: UserIdOf](u: U): Boolean = me.exists(_ is u)

  def userId = me.map(_.id)

  def username = me.map(_.username)

  def usernameOrAnon = username | "Anonymous"

  def troll = me.exists(_.marks.troll)

  def ipAddress = lila.common.HTTPRequest ipAddress req

  def kid   = me.exists(_.kid)
  def noKid = !kid

  def withLang(newLang: Lang) = UserContext(req, me, impersonatedBy, lang)

  override def toString =
    "%s %s %s".format(
      me.fold("Anonymous")(_.username),
      req.remoteAddress,
      req.headers.get("User-Agent") | "?"
    )

final class UserBodyContext[A](val body: Request[A], m: Option[User], i: Option[User], l: Lang)
    extends UserContext(body, m, i, l):

  override def withLang(newLang: Lang): UserBodyContext[A] = UserBodyContext(body, m, i, newLang)
