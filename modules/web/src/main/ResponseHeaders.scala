package lila.web

import play.api.http.HeaderNames
import play.api.mvc.*

import lila.common.HTTPRequest

trait ResponseHeaders extends HeaderNames:

  def headersForApiOrApp(using req: RequestHeader) =
    val appOrigin = HTTPRequest.appOrigin(req)
    List(
      "Access-Control-Allow-Origin" -> appOrigin.getOrElse("*"),
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
    "Allow" -> allowMethods,
    "Access-Control-Max-Age" -> "86400"
  )

  val assetsHeaders = List(
    "Service-Worker-Allowed" -> "/",
    "Cross-Origin-Embedder-Policy" -> "require-corp" // for Stockfish worker
  )

  val permissionsPolicyHeader =
    "Permissions-Policy" -> List(
      "screen-wake-lock=(self \"https://lichess1.org\")",
      "microphone=(self)",
      "fullscreen=(self)"
    ).mkString(", ")

  def lastModified(date: Instant) = LAST_MODIFIED -> date.atZone(utcZone)

  object crossOriginPolicy:

    def isSet(result: Result) = result.header.headers.contains(embedderPolicyHeader)

    def forReq(req: RequestHeader) =
      if supportsCoepCredentialless(req) then credentialless else requireCorp

    def supportsCoepCredentialless(req: RequestHeader) =
      import HTTPRequest.*
      isChrome96Plus(req) || (isFirefox119Plus(req) && !isMobileBrowser(req))

    def supportsCredentiallessIFrames(req: RequestHeader) =
      import HTTPRequest.*
      isChrome113Plus(req)

    def unsafe = headers("unsafe-none")
    def credentialless = headers("credentialless")
    def requireCorp = headers("require-corp")

    private val openerPolicyHeader = "Cross-Origin-Opener-Policy"
    private val embedderPolicyHeader = "Cross-Origin-Embedder-Policy"

    private def headers(policy: "credentialless" | "require-corp" | "unsafe-none") = List(
      openerPolicyHeader -> (if policy == "unsafe-none" then "unsafe-none" else "same-origin"),
      embedderPolicyHeader -> policy
    )
