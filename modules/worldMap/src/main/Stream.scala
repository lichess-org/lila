package lila.worldMap

import akka.actor._
import com.google.common.cache.LoadingCache
import com.sanoma.cda.geoip.{ MaxMindIpGeo, IpLocation }
import lila.hub.actorApi.round.{ Open, Close, DoorEvent }
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.concurrent.duration._

final class Stream(
    system: ActorSystem,
    players: Players,
    geoIp: MaxMindIpGeo,
    geoIpCacheTtl: Duration) {

  system.lilaBus.subscribe(system.actorOf(Props(new Actor {
    def receive = {
      case e: DoorEvent => channel push e
    }
  })), 'roundDoor)

  private val (enumerator, channel) = Concurrent.broadcast[DoorEvent]

  private val ipCache = lila.memo.Builder.cache(geoIpCacheTtl, localizeIp)
  private def localizeIp(ip: String): Option[Location] =
    geoIp getLocation ip flatMap Location.apply

  def processOpen(id: String) = Input.Empty
  // ipCache get move.ip match {
  //   case None => Input.Empty
  //   case Some(loc) =>
  //     val opponentLoc = players.getOpponentLocation(move.gameId, loc)
  //     Input.El(List(
  //       loc.country,
  //       loc.lat,
  //       loc.lon,
  //       opponentLoc.map(_.lat) getOrElse "",
  //       opponentLoc.map(_.lon) getOrElse "",
  //       move.move,
  //       move.piece,
  //       opponentLoc.map(_.country) getOrElse ""
  //     ) mkString "|")
  // }
  def processClose(id: String) = Input.Empty

  private val processor: Enumeratee[DoorEvent, String] =
    Enumeratee.mapInput[DoorEvent].apply[String] {
      case Input.El(Open(id))  => processOpen(id pp "open")
      case Input.El(Close(id)) => processClose(id pp "close")
      case _                   => Input.Empty
    }

  val producer = enumerator &> processor
}

object Stream {

  sealed trait Event
  case class Open(id: String, white: Player, black: Player) extends Event
  case class Close(id: String) extends Event

  case class Player(country: String, lat: Float, long: Float)
}
