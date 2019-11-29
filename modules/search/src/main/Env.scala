package lila.search

import akka.actor.ActorSystem
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import play.api.libs.ws._

case class SearchConfig(
    enabled: Boolean,
    writeable: Boolean,
    endpoint: String
)

final class Env(
    appConfig: Configuration,
    system: ActorSystem,
    ws: WSClient
) {

  private val config = appConfig.get[SearchConfig]("search")(AutoConfig.loader)

  def makeHttp(index: Index): ESClientHttp = wire[ESClientHttp]

  val makeClient = (index: Index) =>
    if (config.enabled) makeHttp(index)
    else wire[ESClientStub]
}
