package lila.app
package http

import play.api.mvc.RequestHeader

import lila.common.HTTPRequest

object ResponseHeaders {

  def headersForApiOrApp(req: RequestHeader) = {
    val appOrigin = HTTPRequest.appOrigin(req)
    List(
      "Access-Control-Allow-Origin"  -> appOrigin.getOrElse("*"),
      "Access-Control-Allow-Methods" -> allowMethods,
      "Access-Control-Allow-Headers" -> {
        List(
          "Origin",
          "Authorization",
          "If-Modified-Since",
          "Cache-Control"
        ) ::: appOrigin.isDefined.??(List("X-Requested-With", "sessionId", "Content-Type"))
      }.mkString(", "),
      "Vary" -> "Origin"
    ) ::: appOrigin.isDefined.??(
      List(
        "Access-Control-Allow-Credentials" -> "true"
      )
    )
  }

  val allowMethods = List("OPTIONS", "GET", "POST", "DELETE") mkString ", "
}
