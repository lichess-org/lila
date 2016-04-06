package lila.worldMap

import com.typesafe.config.Config

import akka.actor._
import com.sanoma.cda.geoip.MaxMindIpGeo
import lila.common.PimpedConfig._

final class Env(
    system: akka.actor.ActorSystem,
    config: Config) {

  private val GeoIPFile = config getString "geoip.file"
  private val GeoIPCacheTtl = config duration "geoip.cache_ttl"

  val stream = system.actorOf(
    Props(new Stream(
      geoIp = MaxMindIpGeo(GeoIPFile, 0),
      geoIpCacheTtl = GeoIPCacheTtl)))

  system.lilaBus.subscribe(stream, 'roundDoor)

  import akka.pattern.ask
  import akka.stream.scaladsl.Source
  import scala.concurrent.duration._
  import play.api.libs.json.JsValue
  implicit val timeout = akka.util.Timeout(5 seconds)
  def getSource: Fu[Stream.SourceType] =
    stream ? Stream.GetSource mapTo manifest[Stream.SourceType]
}

object Env {

  lazy val current: Env = "worldMap" boot new Env(
    system = lila.common.PlayApp.system,
    config = lila.common.PlayApp loadConfig "worldMap")
}

