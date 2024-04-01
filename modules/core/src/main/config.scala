package lila.core
package config

import play.api.ConfigLoader

import lila.common.config.{ *, given }
import lila.common.autoconfig.ConfigName
import lila.common.autoconfig.AutoConfig

case class NetConfig(
    domain: NetDomain,
    prodDomain: NetDomain,
    @ConfigName("base_url") baseUrl: BaseUrl,
    @ConfigName("asset.domain") assetDomain: AssetDomain,
    @ConfigName("asset.base_url") assetBaseUrl: AssetBaseUrl,
    @ConfigName("asset.base_url_internal") assetBaseUrlInternal: AssetBaseUrlInternal,
    @ConfigName("asset.minified") minifiedAssets: Boolean,
    @ConfigName("stage.banner") stageBanner: Boolean,
    @ConfigName("site.name") siteName: String,
    @ConfigName("socket.domains") socketDomains: List[String],
    @ConfigName("socket.alts") socketAlts: List[String],
    crawlable: Boolean,
    @ConfigName("ratelimit") rateLimit: RateLimit,
    email: EmailAddress
):
  def isProd = domain == prodDomain

object NetConfig:
  given ConfigLoader[NetDomain]            = strLoader
  given ConfigLoader[BaseUrl]              = strLoader
  given ConfigLoader[AssetDomain]          = strLoader
  given ConfigLoader[AssetBaseUrl]         = strLoader
  given ConfigLoader[AssetBaseUrlInternal] = strLoader
  given ConfigLoader[RateLimit]            = boolLoader
  given ConfigLoader[NetConfig]            = AutoConfig.loader[NetConfig]
