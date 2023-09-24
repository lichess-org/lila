package lila.app
package http

import play.api.mvc.*
import play.api.http.HeaderNames

import lila.common.HTTPRequest

trait ResponseHeaders extends HeaderNames:

  def headersForApiOrApp(using req: RequestHeader) =
    val appOrigin = HTTPRequest.appOrigin(req)
    List(
      "Access-Control-Allow-Origin"  -> appOrigin.getOrElse("*"),
      "Access-Control-Allow-Methods" -> allowMethods,
      "Access-Control-Allow-Headers" -> {
        List(
          "Origin",
          "Authorization",
          "If-Modified-Since",
          "Cache-Control",
          "Content-Type"
        ) ::: appOrigin.isDefined.so(List("X-Requested-With", "sessionId"))
      }.mkString(", "),
      VARY -> "Origin"
    ) ::: appOrigin.isDefined.so(
      List(
        "Access-Control-Allow-Credentials" -> "true"
      )
    )

  val allowMethods = List("OPTIONS", "GET", "POST", "PUT", "DELETE") mkString ", "
  val optionsHeaders = List(
    "Allow"                  -> allowMethods,
    "Access-Control-Max-Age" -> "86400"
  )

  val assetsHeaders = List(
    "Service-Worker-Allowed"       -> "/",
    "Cross-Origin-Embedder-Policy" -> "require-corp" // for Stockfish worker
  )

  val credentiallessHeaders = List(
    "Cross-Origin-Opener-Policy"   -> "same-origin",
    "Cross-Origin-Embedder-Policy" -> "credentialless"
  )

  val noProxyBufferHeader = "X-Accel-Buffering" -> "no"

  def noProxyBuffer(res: Result) = res.withHeaders(noProxyBufferHeader)
  def asAttachment(name: String)(res: Result) =
    res.withHeaders(CONTENT_DISPOSITION -> s"attachment; filename=$name")
  def asAttachmentStream(name: String)(res: Result) = noProxyBuffer(asAttachment(name)(res))

  def lastModified(date: Instant) = LAST_MODIFIED -> date.atZone(utcZone)
