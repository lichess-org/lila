package controllers

import lila.user.UserContext
import lila.common.Form.trueish
import lila.common.IsMobile

import play.api.mvc.RequestHeader

trait RequestGetter {

  protected def get(name: String)(implicit ctx: UserContext): Option[String] = get(name, ctx.req)

  protected def get(name: String, req: RequestHeader): Option[String] =
    req.queryString get name flatMap (_.headOption) filter (_.nonEmpty)

  protected def getInt(name: String)(implicit ctx: UserContext) =
    get(name) flatMap (_.toIntOption)

  protected def getInt(name: String, req: RequestHeader): Option[Int] =
    req.queryString get name flatMap (_.headOption) flatMap (_.toIntOption)

  protected def getLong(name: String)(implicit ctx: UserContext) =
    get(name) flatMap (_.toLongOption)

  protected def getLong(name: String, req: RequestHeader) =
    get(name, req) flatMap (_.toLongOption)

  protected def getBool(name: String)(implicit ctx: UserContext) =
    (getInt(name) exists trueish) || (get(name) exists trueish)

  protected def getBool(name: String, req: RequestHeader) =
    (getInt(name, req) exists trueish) || (get(name, req) exists trueish)

  protected def getBoolOpt(name: String)(implicit ctx: UserContext) =
    (getInt(name) map trueish) orElse (get(name) map trueish)

  protected def getBoolOpt(name: String, req: RequestHeader) =
    (getInt(name, req) map trueish) orElse (get(name, req) map trueish)

  protected def getMobile(implicit ctx: UserContext) =
    IsMobile(getBool("mobile"))
}
