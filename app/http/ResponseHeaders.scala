package lila.app
package http

import play.api.http.HeaderNames
import play.api.mvc.*

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
  val allowMethods = List("OPTIONS", "GET", "POST", "PUT", "DELETE").mkString(", ")
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

  val permissionsPolicyHeader =
    "Permissions-Policy" -> List(
      "screen-wake-lock=(self \"https://lichess1.org\")",
      "microphone=(self)",
      "fullscreen=(self)"
    ).mkString(", ")

  val noProxyBufferHeader = "X-Accel-Buffering" -> "no"

  def noProxyBuffer(res: Result) = res.withHeaders(noProxyBufferHeader)
  def asAttachment(name: String)(res: Result) =
    res.withHeaders(CONTENT_DISPOSITION -> s"attachment; filename=$name")
  def asAttachmentStream(name: String)(res: Result) = noProxyBuffer(asAttachment(name)(res))

  def lastModified(date: Instant) = LAST_MODIFIED -> date.atZone(utcZone)

  object embedderPolicy:

    def isSet(result: Result) = result.header.headers.contains(embedderPolicyHeader).pp("is set")

    def default        = headers("unsafe-none")
    def credentialless = headers("credentialless")

    private val openerPolicyHeader   = "Cross-Origin-Opener-Policy"
    private val embedderPolicyHeader = "Cross-Origin-Embedder-Policy"

    private def headers(policy: "credentialless" | "require-corp" | "unsafe-none") = List(
      openerPolicyHeader   -> "same-origin",
      embedderPolicyHeader -> policy
    )
