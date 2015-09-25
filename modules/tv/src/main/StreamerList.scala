package lila.tv

import com.typesafe.config.Config
import scala.util.{ Try, Success, Failure }

private[tv] final class StreamerList(config: List[Config]) {

  import StreamerList._

  val streamers: (List[Streamer], List[Exception]) = config.map { c =>
    Try {
      Streamer(
        service = c getString "service" match {
          case s if s == "twitch" => Twitch
          case s if s == "hitbox" => Hitbox
          case s                  => sys error s"Invalid service name: $s"
        },
        streamerName = c getString "streamer_name",
        lichessName = c getString "lichess_name",
        featured = c.getBoolean("featured"),
        chat = c.getBoolean("chat"))
    }
  }.foldLeft(List.empty[Streamer] -> List.empty[Exception]) {
    case ((res, err), Success(r))            => (r :: res, err)
    case ((res, err), Failure(e: Exception)) => (res, e :: err)
    case (_, Failure(e))                     => throw e
  }
  println(streamers)
}

object StreamerList {

  sealed trait Service
  case object Twitch extends Service
  case object Hitbox extends Service

  case class Streamer(
    service: Service,
    streamerName: String,
    lichessName: String,
    featured: Boolean,
    chat: Boolean)
}
