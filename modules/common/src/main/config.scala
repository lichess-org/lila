package lila.common

import lila.common.autoconfig.*
import scala.jdk.CollectionConverters.*
import play.api.ConfigLoader

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

  opaque type Max = Int
  object Max extends OpaqueInt[Max]

  opaque type MaxPerPage = Int
  object MaxPerPage extends OpaqueInt[MaxPerPage]

  opaque type MaxPerSecond = Int
  object MaxPerSecond extends OpaqueInt[MaxPerSecond]

  opaque type NetDomain = String
  object NetDomain extends OpaqueString[NetDomain]

  opaque type AssetDomain = String
  object AssetDomain extends OpaqueString[AssetDomain]

  opaque type AssetBaseUrl = String
  object AssetBaseUrl extends OpaqueString[AssetBaseUrl]

  opaque type RateLimit = Boolean
  object RateLimit extends YesNo[RateLimit]

  opaque type EndpointUrl = String
  object EndpointUrl extends OpaqueString[EndpointUrl]

  case class NetConfig(
      domain: NetDomain,
      prodDomain: NetDomain,
      @ConfigName("base_url") baseUrl: BaseUrl,
      @ConfigName("asset.domain") assetDomain: AssetDomain,
      @ConfigName("asset.base_url") assetBaseUrl: AssetBaseUrl,
      @ConfigName("asset.base_url_internal") assetBaseUrlInternal: String,
      @ConfigName("asset.minified") minifiedAssets: Boolean,
      @ConfigName("stage.banner") stageBanner: Boolean,
      @ConfigName("site.name") siteName: String,
      @ConfigName("socket.domains") socketDomains: List[String],
      crawlable: Boolean,
      @ConfigName("ratelimit") rateLimit: RateLimit,
      email: EmailAddress
  ):
    def isProd = domain == prodDomain

  given ConfigLoader[Secret]       = strLoader(Secret.apply)
  given ConfigLoader[EmailAddress] = strLoader(EmailAddress(_))
  given ConfigLoader[NetConfig]    = AutoConfig.loader[NetConfig]

  given ConfigLoader[List[String]] = ConfigLoader.seqStringLoader.map(_.toList)

  given [A](using l: ConfigLoader[A]): ConfigLoader[List[A]] =
    ConfigLoader { c => k =>
      c.getConfigList(k).asScala.toList map { l.load(_) }
    }

  given [A](using loader: ConfigLoader[A]): ConfigLoader[Option[A]] =
    ConfigLoader[Option[A]](c => k => if c.hasPath(k) then Some(loader.load(c, k)) else None)

  def strLoader[A](f: String => A): ConfigLoader[A]   = ConfigLoader.stringLoader map f
  def intLoader[A](f: Int => A): ConfigLoader[A]      = ConfigLoader.intLoader map f
  def boolLoader[A](f: Boolean => A): ConfigLoader[A] = ConfigLoader.booleanLoader map f
