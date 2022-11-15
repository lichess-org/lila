package lila.common

import scala.concurrent.duration.FiniteDuration
import lila.common.autoconfig.*
import scala.jdk.CollectionConverters.*
import play.api.ConfigLoader

object config:

  case class Every(value: FiniteDuration)  extends AnyVal
  case class AtMost(value: FiniteDuration) extends AnyVal
  case class Delay(value: FiniteDuration)  extends AnyVal

  case class CollName(value: String) extends AnyVal with StringValue

  case class Secret(value: String) extends AnyVal:
    override def toString = "Secret(****)"

  case class BaseUrl(value: String) extends AnyVal with StringValue

  case class AppPath(value: java.io.File) extends AnyVal:
    override def toString = value.toString

  case class Max(value: Int) extends AnyVal with IntValue with Ordered[Int]:
    def compare(other: Int) = Integer.compare(value, other)
    def atMost(max: Int)    = Max(value atMost max)
  case class MaxPerPage(value: Int) extends AnyVal with IntValue

  case class MaxPerSecond(value: Int) extends AnyVal with IntValue

  case class NetDomain(value: String)    extends AnyVal with StringValue
  case class AssetDomain(value: String)  extends AnyVal with StringValue
  case class AssetBaseUrl(value: String) extends AnyVal with StringValue
  case class RateLimit(value: Boolean)   extends AnyVal

  case class NetConfig(
      domain: NetDomain,
      prodDomain: NetDomain,
      @ConfigName("base_url") baseUrl: BaseUrl,
      @ConfigName("asset.domain") assetDomain: AssetDomain,
      @ConfigName("asset.base_url") assetBaseUrl: AssetBaseUrl,
      @ConfigName("asset.minified") minifiedAssets: Boolean,
      @ConfigName("stage.banner") stageBanner: Boolean,
      @ConfigName("site.name") siteName: String,
      @ConfigName("socket.domains") socketDomains: List[String],
      crawlable: Boolean,
      @ConfigName("ratelimit") rateLimit: RateLimit,
      email: EmailAddress
  ):
    def isProd = domain == prodDomain

  given ConfigLoader[Max]          = intLoader(Max.apply)
  given ConfigLoader[MaxPerPage]   = intLoader(MaxPerPage.apply)
  given ConfigLoader[MaxPerSecond] = intLoader(MaxPerSecond.apply)
  given ConfigLoader[CollName]     = strLoader(CollName.apply)
  given ConfigLoader[Secret]       = strLoader(Secret.apply)
  given ConfigLoader[BaseUrl]      = strLoader(BaseUrl.apply)
  given ConfigLoader[EmailAddress] = strLoader(EmailAddress.apply)
  given ConfigLoader[NetDomain]    = strLoader(NetDomain.apply)
  given ConfigLoader[AssetDomain]  = strLoader(AssetDomain.apply)
  given ConfigLoader[AssetBaseUrl] = strLoader(AssetBaseUrl.apply)
  given ConfigLoader[RateLimit]    = boolLoader(RateLimit.apply)
  given ConfigLoader[NetConfig]    = AutoConfig.loader[NetConfig]

  given ConfigLoader[List[String]] = ConfigLoader.seqStringLoader.map(_.toList)

  given [A](using l: ConfigLoader[A]): ConfigLoader[List[A]] =
    ConfigLoader { c => k =>
      c.getConfigList(k).asScala.toList map { l.load(_) }
    }

  given [A](using loader: ConfigLoader[A]): ConfigLoader[Option[A]] =
    ConfigLoader[Option[A]](c => k => if (c.hasPath(k)) Some(loader.load(c, k)) else None)

  def strLoader[A](f: String => A): ConfigLoader[A]              = ConfigLoader.stringLoader map f
  def intLoader[A](f: Int => A): ConfigLoader[A]                 = ConfigLoader.intLoader map f
  def boolLoader[A](f: Boolean => A): ConfigLoader[A]            = ConfigLoader.booleanLoader map f
  def durationLoader[A](f: FiniteDuration => A): ConfigLoader[A] = ConfigLoader.finiteDurationLoader map f
