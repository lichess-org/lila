package controllers

import lila._

import play.api.mvc.RequestHeader

trait RequestGetter {

  def get(name: String)(implicit request: RequestHeader) =
    request.queryString get name flatMap (_.headOption) filter (""!=)

  def getInt(name: String)(implicit request: RequestHeader) =
    get(name)(request) map (_.toInt)

  def getOr(name: String, default: String)(implicit request: RequestHeader) =
    get(name) getOrElse default

  def getIntOr(name: String, default: Int)(implicit request: RequestHeader) =
    getInt(name) getOrElse default
}
