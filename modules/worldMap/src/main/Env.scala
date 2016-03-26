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

  private val stream = system.actorOf(
    Props(new Stream(
      geoIp = MaxMindIpGeo(GeoIPFile, 0),
      geoIpCacheTtl = GeoIPCacheTtl)))
  system.lilaBus.subscribe(stream, 'changeFeaturedGame, 'streams, 'nbMembers, 'nbRounds)

  def getStream = {
    import play.api.libs.iteratee._
    import play.api.libs.json._
    import akka.pattern.ask
    import makeTimeout.short
    stream ? Stream.Get mapTo manifest[Enumerator[JsValue]]
  }
}

object Env {

  lazy val current: Env = "worldMap" boot new Env(
    system = lila.common.PlayApp.system,
    config = lila.common.PlayApp loadConfig "worldMap")
}

