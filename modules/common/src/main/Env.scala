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
  appConfig.underlying.getString("net.asset.domain")
  val c = appConfig.underlying.getConfig("net")
  println(c)
  println(c getString "asset.domain")
  val netConfig = appConfig.get[NetConfig]("net")
  def netDomain = netConfig.domain

  private lazy val detectLanguageConfig =
    appConfig.get[DetectLanguage.Config]("detect_language.api")

  lazy val detectLanguage = wire[DetectLanguage]
}
