package lila.worldMap

import akka.actor._
import com.google.common.cache.LoadingCache
import com.sanoma.cda.geoip.{ MaxMindIpGeo, IpLocation }
import lila.hub.actorApi.round.SocketEvent
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.concurrent.duration._

import lila.rating.PerfType

private final class Stream(
    geoIp: MaxMindIpGeo,
    geoIpCacheTtl: Duration) extends Actor {

  import Stream.gameWriter

  override def preStart() {
    context.system.lilaBus.subscribe(self, 'roundDoor)
  }

  val games = scala.collection.mutable.Map.empty[String, Stream.Game]

  def receive = {
    case SocketEvent.OwnerJoin(id, color, ip) =>
      ipCache get ip foreach { point =>
        val game = games get id match {
          case Some(game) => game withPoint point
          case None       => Stream.Game(id, List(point))
        }
        games += (id -> game)
        channel push Stream.Event.Add(game)
      }
    case SocketEvent.Stop(id) =>
      games -= id
      channel push Stream.Event.Remove(id)
    case Stream.Get => sender ! {
      Enumerator enumerate games.values.map(gameWriter.writes) andThen producer
    }
  }

  val (enumerator, channel) = Concurrent.broadcast[Stream.Event]

  val producer = enumerator &> Enumeratee.map[Stream.Event].apply[JsValue] {
    case Stream.Event.Add(game)  => Json toJson game
    case Stream.Event.Remove(id) => Json.obj("id" -> id)
  }

  val ipCache = lila.memo.Builder.cache(geoIpCacheTtl, ipToPoint)
  def ipToPoint(ip: String): Option[Stream.Point] =
    geoIp getLocation ip flatMap Location.apply map { loc =>
      Stream.Point(loc.lat, loc.lon)
    }
}

object Stream {

  case object Get

  case class Game(id: String, points: List[Point]) {

    def withPoint(point: Point) =
      if (points contains point) this
      else copy(points = point :: points.take(1))
  }

  private def truncate(d: Double) = lila.common.Maths.truncateAt(d, 4)

  private implicit def gameWriter: Writes[Game] = Writes { game =>
    Json.obj(
      "id" -> game.id,
      "ps" -> Json.toJson {
        game.points.map { p =>
          List(p.lat, p.lon) map truncate
        }
      }
    )
  }

  case class Point(lat: Double, lon: Double)

  sealed trait Event
  object Event {
    case class Add(game: Game) extends Event
    case class Remove(id: String) extends Event
  }
}
