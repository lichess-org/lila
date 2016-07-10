package controllers

import lila.api._
import lila.user.UserContext

import play.api.mvc.RequestHeader

trait RequestGetter {

  protected def get(name: String)(implicit ctx: UserContext): Option[String] = get(name, ctx.req)

  protected def get(name: String, req: RequestHeader): Option[String] =
    req.queryString get name flatMap (_.headOption) filter (_.nonEmpty)

  protected def getInt(name: String)(implicit ctx: UserContext) =
    get(name)(ctx) flatMap parseIntOption

  protected def getInt(name: String, req: RequestHeader): Option[Int] =
    req.queryString get name flatMap (_.headOption) flatMap parseIntOption

  protected def getBool(name: String)(implicit ctx: UserContext) =
    getInt(name) contains 1

  protected def getBool(name: String, req: RequestHeader) =
    getInt(name, req) contains 1

  protected def getBoolOpt(name: String)(implicit ctx: UserContext) =
    getInt(name) map (1==)

  protected def getBoolOpt(name: String, req: RequestHeader) =
    getInt(name, req) map (1==)
}
