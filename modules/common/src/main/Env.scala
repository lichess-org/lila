package lila.common

import com.softwaremill.macwire._
import play.api.Configuration
import play.api.libs.ws.WSClient

import config._

@Module
final class Env(
    appConfig: Configuration,
    ws: WSClient
)(implicit ec: scala.concurrent.ExecutionContext) {

  val netConfig = appConfig.get[NetConfig]("net")
  def netDomain = netConfig.domain

  lazy val detectLanguage = new DetectLanguage(ws, appConfig.get[DetectLanguage.Config]("detectlanguage.api"))
}
