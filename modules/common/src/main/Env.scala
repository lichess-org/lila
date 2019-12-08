package lila.common

import com.softwaremill.macwire._
import play.api.Configuration
import play.api.libs.ws.WSClient

import config._

@Module
final class Env(
    appConfig: Configuration,
    ws: WSClient
) {

  val netConfig = appConfig.get[NetConfig]("net")
  def netDomain = netConfig.domain

  private lazy val detectLanguageConfig =
    appConfig.get[DetectLanguage.Config]("detectlanguage.api")

  lazy val detectLanguage = wire[DetectLanguage]
}
