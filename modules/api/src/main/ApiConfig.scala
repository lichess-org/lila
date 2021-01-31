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
    val accessibility: ApiConfig.Accessibility
)

object ApiConfig {

  final class Accessibility(
      val blindCookieName: String,
      val blindCookieMaxAge: FiniteDuration,
      blindCookieSalt: Secret
  ) {
    def hash(implicit ctx: lila.user.UserContext) = {
      import com.roundeights.hasher.Implicits._
      (ctx.userId | "anon").salt(blindCookieSalt.value).md5.hex
    }
  }

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
        c.get[FiniteDuration]("accessibility.blind.cookie.max_age"),
        c.get[Secret]("accessibility.blind.cookie.salt")
      )
    )
}
