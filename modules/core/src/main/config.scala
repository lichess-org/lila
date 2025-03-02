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

  opaque type AssetBaseUrlInternal = String
  object AssetBaseUrlInternal extends OpaqueString[AssetBaseUrlInternal]

  opaque type RateLimit = Boolean
  object RateLimit extends YesNo[RateLimit]

  case class Credentials(user: String, password: Secret):
    def show = s"$user:${password.value}"
  object Credentials:
    def read(str: String): Option[Credentials] = str.split(":") match
      case Array(user, password) => Credentials(user, Secret(password)).some
      case _                     => none

  case class HostPort(host: String, port: Int):
    def show = s"$host:$port"
  object HostPort:
    def read(str: String): Option[HostPort] = str.split(":") match
      case Array(host, port) => port.toIntOption.map(HostPort(host, _))
      case _                 => none

  case class NetConfig(
      domain: NetDomain,
      prodDomain: NetDomain,
      baseUrl: BaseUrl,
      assetDomain: AssetDomain,
      assetBaseUrl: AssetBaseUrl,
      assetBaseUrlInternal: AssetBaseUrlInternal,
      minifiedAssets: Boolean,
      externalManifest: Boolean,
      stageBanner: Boolean,
      siteName: String,
      socketDomains: List[String],
      socketAlts: List[String],
      crawlable: Boolean,
      rateLimit: RateLimit,
      email: EmailAddress,
      logRequests: Boolean
  ):
    def isProd = domain == prodDomain
