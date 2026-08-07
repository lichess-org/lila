package lila.core

import lila.core.email.EmailAddress

object config:

  opaque type Every = FiniteDuration
  object Every extends OpaqueDuration[Every]
  opaque type AtMost = FiniteDuration
  object AtMost extends OpaqueDuration[AtMost]
  opaque type Delay = FiniteDuration
  object Delay extends OpaqueDuration[Delay]

  opaque type CollName = String
  object CollName extends OpaqueString[CollName]

  case class Secret(value: String) extends AnyVal:
    override def toString = "Secret(****)"

  opaque type BaseUrl = String
  object BaseUrl extends OpaqueString[BaseUrl]

  opaque type NetDomain = String
  object NetDomain extends OpaqueString[NetDomain]

  opaque type AssetDomain = String
  object AssetDomain extends OpaqueString[AssetDomain]

  opaque type AssetBaseUrl = String
  object AssetBaseUrl extends OpaqueString[AssetBaseUrl]

  opaque type RateLimit = Boolean
  object RateLimit extends YesNo[RateLimit]

  type RouteUrl = play.api.mvc.Call => data.Url

  case class NetConfig(
      domain: NetDomain,
      prodDomain: NetDomain,
      baseUrl: BaseUrl,
      assetDomain: AssetDomain,
      assetBaseUrl: AssetBaseUrl,
      stageBanner: Boolean,
      siteName: String,
      socketDomains: List[String],
      socketAlts: List[String],
      crawlable: Boolean,
      rateLimit: RateLimit,
      email: EmailAddress,
      logRequests: Boolean
  ):
    def routeUrl(call: play.api.mvc.Call) = data.Url(s"${baseUrl}${call.url}")

  opaque type ImageGetOrigin = String
  object ImageGetOrigin extends OpaqueString[ImageGetOrigin]
