package controllers

import play.api.mvc.Request

trait RequestGetter {

  def get(name: String)(implicit request: Request[_]) =
    request.queryString get name flatMap (_.headOption)

  def getInt(name: String)(implicit request: Request[_]) =
    get(name)(request) map (_.toInt)

  def getOr(name: String, default: String)(implicit request: Request[_]) =
    get(name) getOrElse default

  def getIntOr(name: String, default: Int)(implicit request: Request[_]) =
    getInt(name) getOrElse default
}
