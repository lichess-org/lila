package lila.api

import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import lila.common.config._
import scala.concurrent.duration._

import lila.common.config._

@Module
final class ApiConfig(
    @ConfigName("api.token") val apiToken: Secret,
    @ConfigName("api.influx_event.endpoint") val influxEventEndpoint: String,
    @ConfigName("api.influx_event.env") val influxEventEnv: String,
    @ConfigName("app.stage") val isStage: Boolean,
    @ConfigName("prismic.api_url") val prismicApiUrl: String,
    @ConfigName("editor.animation.duration") val editorAnimationDuration: FiniteDuration,
    @ConfigName("explorer.endpoint") val explorerEndpoint: String,
    @ConfigName("explorer.tablebase.endpoint") val tablebaseEndpoint: String,
    accessibility: ApiConfig.Accessibility
)

object ApiConfig {

  final class Accessibility(
      @ConfigName("blind.cookie.name") val blindCookieName: String,
      @ConfigName("blind.cookie.max_age") val blindCookieMaxAge: FiniteDuration,
      @ConfigName("blind.cookie.salt") blindCookieSalt: Secret
  ) {
    def hash(implicit ctx: lila.user.UserContext) = {
      import com.roundeights.hasher.Implicits._
      (ctx.userId | "anon").salt(blindCookieSalt.value).md5.hex
    }
  }
  private implicit val accessibilityLoader = AutoConfig.loader[Accessibility]
  implicit val loader = AutoConfig.loader[ApiConfig]
}
