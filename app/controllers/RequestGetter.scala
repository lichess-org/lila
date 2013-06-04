package controllers

import lila.api._
import lila.user.Context

import play.api.mvc.RequestHeader

trait RequestGetter {

  protected def get(name: String)(implicit ctx: Context): Option[String] = get(name, ctx.req)

  protected def get(name: String, req: RequestHeader): Option[String] =
    req.queryString get name flatMap (_.headOption) filter (""!=)

  protected def getInt(name: String)(implicit ctx: Context) =
    get(name)(ctx) flatMap parseIntOption

  protected def getInt(name: String, req: RequestHeader): Option[Int] =
    req.queryString get name flatMap (_.headOption) flatMap parseIntOption
}
