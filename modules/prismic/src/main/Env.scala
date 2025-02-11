package lila.prismic

import play.api.Configuration

import com.softwaremill.macwire._
import io.methvin.play.autoconfig._

private class PrismicConfig(
    @ConfigName("api_url") val apiUrl: String,
)

@Module
final class Env(
    appConfig: Configuration,
)(implicit
    ec: scala.concurrent.ExecutionContext,
    ws: play.api.libs.ws.WSClient,
) {

  private val config = appConfig.get[PrismicConfig]("prismic")(AutoConfig.loader)

  lazy val prismic = wire[Prismic]

}
