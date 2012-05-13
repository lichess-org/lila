package controllers

import lila._

import play.api.mvc.RequestHeader

trait RequestGetter {

  def get(name: String)(implicit req: RequestHeader) =
    req.queryString get name flatMap (_.headOption) filter (""!=)

  def getInt(name: String)(implicit req: RequestHeader) =
    get(name)(req) map (_.toInt)

  def getOr(name: String, default: String)(implicit req: RequestHeader) =
    get(name) getOrElse default

  def getIntOr(name: String, default: Int)(implicit req: RequestHeader) =
    getInt(name) getOrElse default
}
