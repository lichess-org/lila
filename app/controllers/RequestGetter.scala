package controllers

import lila._
import http.Context

import play.api.mvc.RequestHeader

trait RequestGetter {

  def get(name: String)(implicit ctx: Context): Option[String] = get(name, ctx.req)

  def get(name: String, req: RequestHeader): Option[String] =
    req.queryString get name flatMap (_.headOption) filter (""!=)

  def getInt(name: String)(implicit ctx: Context) =
    get(name)(ctx) map (_.toInt)

  def getOr(name: String, default: String)(implicit ctx: Context) =
    get(name)(ctx) getOrElse default

  def getIntOr(name: String, default: Int)(implicit ctx: Context) =
    getInt(name)(ctx) getOrElse default
}
