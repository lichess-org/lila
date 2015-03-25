package lila.worldMap

import akka.actor._
import com.google.common.cache.LoadingCache
import com.sanoma.cda.geoip.{ MaxMindIpGeo, IpLocation }
import java.io.File
import lila.hub.actorApi.round.MoveEvent
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.concurrent.duration._

final class Stream(
    system: ActorSystem,
    players: Players,
    geoIp: MaxMindIpGeo,
    geoIpCacheTtl: Duration) {

  private val (enumerator, channel) = Concurrent.broadcast[MoveEvent]

  private val ipCache = lila.memo.Builder.cache(geoIpCacheTtl, localizeIp)
  private def localizeIp(ip: String): Option[Location] =
    geoIp getLocation ip flatMap Location.apply

  def processMove(move: MoveEvent) =
    ipCache get move.ip match {
      case None => Input.Empty
      case Some(loc) =>
        val opponentLoc = players.getOpponentLocation(move.gameId, loc)
        Input.El(Json.stringify {
          Json.obj(
            "country" -> loc.country,
            "lat" -> loc.lat,
            "lon" -> loc.lon,
            "oLat" -> opponentLoc.map(_.lat),
            "oLon" -> opponentLoc.map(_.lon),
            "move" -> move.move
          )
        })
    }

  private val processor: Enumeratee[MoveEvent, String] =
    Enumeratee.mapInput[MoveEvent].apply[String] {
      case Input.El(move) => processMove(move)
      case _              => Input.Empty
    }

  val producer = enumerator &> processor

  system.lilaBus.subscribe(system.actorOf(Props(new Actor {
    def receive = {
      case move: MoveEvent => channel push move
    }
  })), 'moveEvent)
}
