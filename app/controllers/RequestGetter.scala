package controllers

import lila._
import http.Context

import play.api.mvc.RequestHeader

trait RequestGetter {

  protected def get(name: String)(implicit ctx: Context): Option[String] = get(name, ctx.req)

  protected def get(name: String, req: RequestHeader): Option[String] =
    req.queryString get name flatMap (_.headOption) filter (""!=)

  protected def getInt(name: String)(implicit ctx: Context) =
    get(name)(ctx) map (_.toInt)

  protected def getOr(name: String, default: String)(implicit ctx: Context) =
    get(name)(ctx) getOrElse default

  protected def getIntOr(name: String, default: Int)(implicit ctx: Context) =
    getInt(name)(ctx) getOrElse default
}
