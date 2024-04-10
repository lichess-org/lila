package lila.app
package http

import play.api.http.HeaderNames
import play.api.mvc.*
import play.api.libs.typedmap.{ TypedKey, TypedMap }

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

  def embedderPolicy                   = ResponseHeaders.embedderPolicy
  def actionSupportsCoepCredentialless = ResponseHeaders.actionSupportsCoepCredentialless
  def disableCoepCredentialless        = ResponseHeaders.disableCoepCredentialless

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

object ResponseHeaders:
  private val disableCoepCrentiallessKey = TypedKey[Unit]("no-coep-credentialless")

  def actionSupportsCoepCredentialless(result: Result) =
    result.attrs.get(ResponseHeaders.disableCoepCrentiallessKey).isEmpty
  // no idea if there's a better way to get a flag to HttpFilter.
  def disableCoepCredentialless(result: Result) =
    result.withAttrs(TypedMap(ResponseHeaders.disableCoepCrentiallessKey -> ()))

  def embedderPolicy(policy: "credentialless" | "require-corp") = List(
    "Cross-Origin-Opener-Policy"   -> "same-origin",
    "Cross-Origin-Embedder-Policy" -> policy
  )
