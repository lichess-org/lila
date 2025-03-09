package lila.web

import play.api.mvc.RequestHeader
import play.api.{ Configuration, ConfigLoader }

import lila.common.config.given
import lila.core.config.*
import lila.core.security.LilaCookie

final class WebConfig(
    val apiToken: Secret,
    val influxEventEndpoint: String,
    val influxEventEnv: String,
    val prometheusKey: String,
    val pagerDuty: WebConfig.PagerDuty
)

object WebConfig:

  object blindCookie:
    val name   = "mBzamRgfXgRBSnXB"
    val maxAge = 365.days
    def make(lilaCookie: LilaCookie)(enable: Boolean)(using RequestHeader) = lilaCookie.cookie(
      name,
      enable.so("1"),
      maxAge = maxAge.toSeconds.toInt.some,
      httpOnly = true.some
    )

  final class PagerDuty(val serviceId: String, val apiKey: Secret)

  def loadFrom(c: play.api.Configuration) =
    WebConfig(
      c.get[Secret]("api.token"),
      c.get[String]("api.influx_event.endpoint"),
      c.get[String]("api.influx_event.env"),
      c.get[String]("kamon.prometheus.lilaKey"),
      new PagerDuty(
        c.get[String]("pagerDuty.serviceId"),
        c.get[Secret]("pagerDuty.apiKey")
      )
    )

  def analyseEndpoints(c: play.api.Configuration) =
    lila.ui.AnalyseEndpoints(
      explorer = c.get[String]("explorer.endpoint"),
      tablebase = c.get[String]("explorer.tablebase_endpoint"),
      externalEngine = c.get[String]("externalEngine.endpoint")
    )

  def netConfig(c: Configuration) = NetConfig(
    domain = c.get[NetDomain]("net.domain"),
    prodDomain = c.get[NetDomain]("net.prodDomain"),
    baseUrl = c.get[BaseUrl]("net.base_url"),
    assetDomain = c.get[AssetDomain]("net.asset.domain"),
    assetBaseUrl = c.get[AssetBaseUrl]("net.asset.base_url"),
    assetBaseUrlInternal = c.get[AssetBaseUrlInternal]("net.asset.base_url_internal"),
    minifiedAssets = c.get[Boolean]("net.asset.minified"),
    externalManifest = c.get[Boolean]("net.asset.external_manifest"),
    stageBanner = c.get[Boolean]("net.stage.banner"),
    siteName = c.get[String]("net.site.name"),
    socketDomains = c.get[List[String]]("net.socket.domains"),
    socketAlts = c.get[List[String]]("net.socket.alts"),
    crawlable = c.get[Boolean]("net.crawlable"),
    rateLimit = c.get[RateLimit]("net.ratelimit"),
    email = c.get[EmailAddress]("net.email"),
    logRequests = c.get[Boolean]("net.http.log")
  )
