package lila.worldMap

import com.typesafe.config.Config

import com.sanoma.cda.geoip.MaxMindIpGeo
import lila.common.PimpedConfig._

final class Env(
  system: akka.actor.ActorSystem,
  config: Config) {

  private val GeoIPFile = config getString "geoip.file"
  private val GeoIPCacheSize = config getInt "geoip.cache_size"
  private val PlayersCacheSize = config getInt "players.cache_size"

  lazy val players = new Players(PlayersCacheSize)

  lazy val stream = new Stream(
    system = system,
    players = players,
    geoIp = MaxMindIpGeo(GeoIPFile, GeoIPCacheSize))
}

object Env {

  lazy val current: Env = "[boot] worldMap" describes new Env(
    system = lila.common.PlayApp.system,
    config = lila.common.PlayApp loadConfig "worldMap")
}

