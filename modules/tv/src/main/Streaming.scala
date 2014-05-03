package lila.tv

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.ws.WS

private final class Streaming(system: ActorSystem) {

  import Streaming._
  import Twitch.Reads._

  def onAir: Fu[List[StreamOnAir]] = {
    import makeTimeout.short
    actor ? Get mapTo manifest[List[StreamOnAir]]
  }

  private[tv] val actor = system.actorOf(Props(new Actor {

    private var onAir = List[StreamOnAir]()

    def receive = {

      case Get => sender ! onAir

      case Search => WS.url("https://api.twitch.tv/kraken/search/streams")
        .withQueryString("q" -> "lichess")
        .withHeaders("Accept" -> "application/vnd.twitchtv.v2+json")
        .get() map { res =>
          res.json.asOpt[Twitch.Result] ?? (_.streams map (_.channel) map { s =>
            StreamOnAir(
              name = s.status,
              url = s.url,
              author = s.display_name)
          })
        } map StreamsOnAir.apply pipeTo self

      case StreamsOnAir(streams) => onAir = streams
    }
  }))

  actor ! Search
}

object Streaming {

  private case object Get
  private[tv] case object Search
}
