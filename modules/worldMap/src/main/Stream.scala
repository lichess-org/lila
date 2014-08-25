package lila.worldMap

import akka.actor._
import com.google.common.cache.LoadingCache
import com.sanoma.cda.geoip.{ MaxMindIpGeo, IpLocation }
import java.io.File
import lila.hub.actorApi.round.MoveEvent
import play.api.libs.iteratee._
import play.api.libs.json._

final class Stream(
    system: ActorSystem,
    players: Players,
    geoIp: MaxMindIpGeo) {

  private val (enumerator, channel) = Concurrent.broadcast[MoveEvent]

  private val processor: Enumeratee[MoveEvent, String] =
    Enumeratee.mapInput[MoveEvent].apply[String] {
      case Input.El(move) =>
        geoIp getLocation move.ip flatMap Location.apply match {
          case None => Input.Empty
          case Some(loc) =>
            val opponentLoc = players.getOpponentLocation(move.gameId, loc)
            Input.El(Json.stringify {
              Json.obj(
                "country" -> loc.country,
                "lat" -> loc.lat,
                "lon" -> loc.lon,
                "oLat" -> opponentLoc.map(_.lat),
                "oLon" -> opponentLoc.map(_.lon)
              )
            })
        }
      case _ => Input.Empty
    }

  val producer = enumerator &> processor

  system.lilaBus.subscribe(system.actorOf(Props(new Actor {
    def receive = {
      case move: MoveEvent => channel push move
    }
  })), 'moveEvent)
}
