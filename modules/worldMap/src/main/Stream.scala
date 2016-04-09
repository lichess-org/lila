package lila.worldMap

import akka.actor._
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

  import Stream._
  implicit val mat = context.system.lilaMat

  val games = scala.collection.mutable.Map.empty[String, Game]

  val clients = scala.collection.mutable.Set.empty[ActorRef]

  def preload = Source.combine(
    Source[JsValue](games.values.map(game2json(makeMd5)).toList),
    Source[JsValue](List(Json.obj("loadComplete" -> true)))
  )(_ => Concat())

  def sendToClients(json: => JsValue) =
    if (clients.nonEmpty) {
      clients.foreach { _ ! json }
    }

  def receive = {

    case SocketEvent.OwnerJoin(id, color, ip) =>
      ipCache get ip foreach { point =>
        val game = games get id match {
          case Some(game) => game withPoint point
          case None       => Game(id, List(point))
        }
        games += (id -> game)
        sendToClients(game2json(makeMd5)(game))
      }

    case SocketEvent.Stop(id) =>
      games -= id
      sendToClients(Json.obj("id" -> id))

    case GetSource =>
      val (client, source) = lila.common.AkkaStream.actorSource[JsValue](100)
      context.watch(client)
      clients += client
      sender ! Source.combine(preload, source)(_ => Concat())

    case Terminated(client) =>
      context.unwatch(client)
      clients -= client
  }

  def makeMd5 = MessageDigest getInstance "MD5"
  val ipCache = lila.memo.Builder.cache(geoIpCacheTtl, ipToPoint)
  def ipToPoint(ip: String): Option[Point] =
    geoIp getLocation ip flatMap toPoint
}

object Stream {

  case object GetSource

  type SourceType = Source[JsValue, akka.NotUsed]

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
}
