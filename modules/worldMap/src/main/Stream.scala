package lila.worldMap

import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import com.sanoma.cda.geoip.{ MaxMindIpGeo, IpLocation }
import java.security.MessageDigest
import lila.hub.actorApi.round.SocketEvent
import play.api.libs.json._
import scala.concurrent.duration._

import lila.rating.PerfType

private final class Stream(
    geoIp: MaxMindIpGeo,
    geoIpCacheTtl: Duration) extends Actor {

  import Stream.game2json

  val games = scala.collection.mutable.Map.empty[String, Stream.Game]

  val (out, realtime) =
    lila.common.AkkaStream.actorSource(20, OverflowStrategy.dropHead)(materializer(context.system))

  def preload = Source[JsValue](games.values.map(game2json(makeMd5)).toList)
  val preloadComplete = Source[JsValue](List(Json.obj("loadComplete" -> true): JsValue))

  val transformer = Flow.fromFunction[Stream.Event, JsValue] {
    case Stream.Event.Add(game)  => game2json(makeMd5)(game)
    case Stream.Event.Remove(id) => Json.obj("id" -> id)
  }

  def makeSource =
    Source.combine(preload, preloadComplete, realtime via transformer)(_ => Concat())

  def makePublisher =
    makeSource.toMat(Sink asPublisher false)(Keep.both).run()

  def receive = {

    case SocketEvent.OwnerJoin(id, color, ip) =>
      ipCache get ip foreach { point =>
        val game = games get id match {
          case Some(game) => game withPoint point
          case None       => Stream.Game(id, List(point))
        }
        games += (id -> game)
        out ! Stream.Event.Add(game)
      }

    case SocketEvent.Stop(id) =>
      games -= id
      out ! Stream.Event.Remove(id)

    case Stream.GetPublisher => sender ! makePublisher
  }

  implicit val mat = materializer(context.system)

  def makeMd5 = MessageDigest getInstance "MD5"
  val ipCache = lila.memo.Builder.cache(geoIpCacheTtl, ipToPoint)
  def ipToPoint(ip: String): Option[Stream.Point] =
    geoIp getLocation ip flatMap Stream.toPoint
}

object Stream {

  case object GetPublisher

  import org.reactivestreams.Publisher
  type PublisherType = Publisher[JsValue]

  case class Game(id: String, points: List[Point]) {

    def withPoint(point: Point) =
      if (points contains point) this
      else copy(points = point :: points.take(1))
  }

  private def truncate(d: Double) = lila.common.Maths.truncateAt(d, 4)

  private val bytes2base64 = java.util.Base64.getEncoder.encodeToString _
  private def game2json(md5: MessageDigest)(game: Game): JsValue = Json.obj(
    "id" -> bytes2base64(md5.digest(game.id getBytes "UTF-8") take 6),
    "ps" -> Json.toJson {
      game.points.map { p =>
        List(p.lat, p.lon) map truncate
      }
    }
  )

  case class Point(lat: Double, lon: Double)
  def toPoint(ipLoc: IpLocation): Option[Point] = ipLoc.geoPoint map { p =>
    Point(p.latitude, p.longitude)
  }

  sealed trait Event
  object Event {
    case class Add(game: Game) extends Event
    case class Remove(id: String) extends Event
  }
}
