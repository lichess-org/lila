package lila.web

import play.api.mvc.RequestHeader

import lila.common.config.given
import lila.core.config.*
import lila.core.security.LilaCookie

final class WebConfig(
    val apiToken: Secret,
    val influxEventEndpoint: String,
    val influxEventEnv: String,
    val pagerDuty: WebConfig.PagerDuty
)

object WebConfig:

  object blindCookie:
    val name   = "mBzamRgfXgRBSnXB"
    val maxAge = 365 days
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
      new PagerDuty(
        c.get[String]("pagerDuty.serviceId"),
        c.get[Secret]("pagerDuty.apiKey")
      )
    )
