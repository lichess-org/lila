package lila.common

import play.api.http.HeaderNames
import play.api.mvc.RequestHeader

import lila.common.HTTPRequest.{ isLocalApp, localAppOrigin }

object ResponseHeaders {

  def headersForApiOrLocalApp(req: RequestHeader) = {
    val isApp = isLocalApp(req)
    List(
      "Access-Control-Allow-Origin" -> { if (isApp) localAppOrigin else "*" },
      "Access-Control-Allow-Methods" -> allowMethods,
      "Access-Control-Allow-Headers" -> {
        List(
          "Origin", "Authorization", "If-Modified-Since", "Cache-Control"
        ) ::: isLocalApp(req).??(List("X-Requested-With", "sessionId", "Content-Type"))
      }.mkString(", "),
      "Vary" -> "Origin"
    ) ::: isApp.??(List(
        "Access-Control-Allow-Credentials" -> "true"
      ))
  }

  val allowMethods = List("OPTIONS", "GET", "POST") mkString ", "
}
