package lila
package http

import user.User
import security.{ Permission, Granter }

import play.api.mvc.{ Request, RequestHeader }

sealed abstract class Context(val req: RequestHeader, val me: Option[User]) {

  def isAuth = me.isDefined

  def isAnon = !isAuth

  def canSeeChat = ~me.map(!_.isChatBan)

  def isGranted(permission: Permission): Boolean = me.fold(Granter(permission), false)

  def is(user: User): Boolean = me == Some(user)

  def userId = me map (_.id)

  override def toString = "%s %s %s".format(
    me.fold(_.username, "Anonymous"),
    req.remoteAddress, 
    req.headers.get("User-Agent") | "?"
  )
}

final class BodyContext(val body: Request[_], m: Option[User]) 
extends Context(body, m) 

final class HeaderContext(r: RequestHeader, m: Option[User]) 
extends Context(r, m)

object Context {

  def apply(req: RequestHeader, me: Option[User]): HeaderContext = 
    new HeaderContext(req, me)

  def apply(req: Request[_], me: Option[User]): BodyContext = 
    new BodyContext(req, me)
}
