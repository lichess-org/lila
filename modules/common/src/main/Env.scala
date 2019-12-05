package lila.common

import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import play.api.libs.ws.WSClient

import config._

@Module
final class Env(
    appConfig: Configuration,
    ws: WSClient
) {

  val netConfig = appConfig.get[NetConfig]("net")

  private lazy val detectLanguageConfig =
    appConfig.get[DetectLanguage.Config]("detect_language.api")

  lazy val detectLanguage = wire[DetectLanguage]
}
