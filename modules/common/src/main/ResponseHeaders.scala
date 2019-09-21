package lila.common

import play.api.http.HeaderNames
import play.api.mvc.RequestHeader

import lila.common.HTTPRequest.{ isLocalApp, isIonicApp, localAppOrigin, ionicAppOrigin }

object ResponseHeaders {

  def headersForApiOrApp(req: RequestHeader) = {
    val isApp = isLocalApp(req) || isIonicApp(req)
    List(
      "Access-Control-Allow-Origin" -> {
        if (isLocalApp(req)) localAppOrigin
        else if (isIonicApp(req)) ionicAppOrigin
        else "*"
      },
      "Access-Control-Allow-Methods" -> allowMethods,
      "Access-Control-Allow-Headers" -> {
        List(
          "Origin", "Authorization", "If-Modified-Since", "Cache-Control"
        ) ::: isApp.??(List("X-Requested-With", "sessionId", "Content-Type"))
      }.mkString(", "),
      "Vary" -> "Origin"
    ) ::: isApp.??(List(
        "Access-Control-Allow-Credentials" -> "true"
      ))
  }

  val allowMethods = List("OPTIONS", "GET", "POST") mkString ", "
}
