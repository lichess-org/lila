package lila.api

import scala.concurrent.duration._

import lila.common.config._

final class ApiConfig(
    val apiToken: Secret,
    val influxEventEndpoint: String,
    val influxEventEnv: String,
    val isStage: Boolean,
    val prismicApiUrl: String,
    val explorerEndpoint: String,
    val tablebaseEndpoint: String,
    val accessibility: ApiConfig.Accessibility,
    val pagerDuty: ApiConfig.PagerDuty
)

object ApiConfig {

  final class Accessibility(
      val blindCookieName: String,
      blindCookieSalt: Secret
  ) {
    val blindCookieMaxAge = 365 days
    def hash(implicit ctx: lila.user.UserContext) = {
      import com.roundeights.hasher.Implicits._
      (ctx.userId | "anon").salt(blindCookieSalt.value).md5.hex
    }
  }

  final class PagerDuty(val serviceId: String, val apiKey: Secret)

  def loadFrom(c: play.api.Configuration) =
    new ApiConfig(
      c.get[Secret]("api.token"),
      c.get[String]("api.influx_event.endpoint"),
      c.get[String]("api.influx_event.env"),
      c.get[Boolean]("app.stage"),
      c.get[String]("prismic.api_url"),
      c.get[String]("explorer.endpoint"),
      c.get[String]("explorer.tablebase.endpoint"),
      new Accessibility(
        c.get[String]("accessibility.blind.cookie.name"),
        c.get[Secret]("accessibility.blind.cookie.salt")
      ),
      new PagerDuty(
        c.get[String]("pagerDuty.serviceId"),
        c.get[Secret]("pagerDuty.apiKey")
      )
    )
}
