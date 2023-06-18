package lila.api

import lila.common.config.*

final class ApiConfig(
    val apiToken: Secret,
    val influxEventEndpoint: String,
    val influxEventEnv: String,
    val prismicApiUrl: String,
    val accessibility: ApiConfig.Accessibility,
    val pagerDuty: ApiConfig.PagerDuty
)

object ApiConfig:

  final class Accessibility(
      val blindCookieName: String,
      blindCookieSalt: Secret
  ):
    val blindCookieMaxAge = 365 days
    def hash(using me: Option[lila.user.Me]) =
      import com.roundeights.hasher.Implicits.*
      me.fold("anon")(_.userId.value).salt(blindCookieSalt.value).md5.hex

  final class PagerDuty(val serviceId: String, val apiKey: Secret)

  def loadFrom(c: play.api.Configuration) =
    ApiConfig(
      c.get[Secret]("api.token"),
      c.get[String]("api.influx_event.endpoint"),
      c.get[String]("api.influx_event.env"),
      c.get[String]("prismic.api_url"),
      new Accessibility(
        c.get[String]("accessibility.blind.cookie.name"),
        c.get[Secret]("accessibility.blind.cookie.salt")
      ),
      new PagerDuty(
        c.get[String]("pagerDuty.serviceId"),
        c.get[Secret]("pagerDuty.apiKey")
      )
    )
