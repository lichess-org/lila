package lila.tv

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.ws.WS

private final class Streaming(
    system: ActorSystem,
    ustreamApiKey: String) {

  import Streaming._
  import Twitch.Reads._
  import Ustream.Reads._

  def onAir: Fu[List[StreamOnAir]] = {
    import makeTimeout.short
    actor ? Get mapTo manifest[List[StreamOnAir]]
  }

  private[tv] val actor = system.actorOf(Props(new Actor {

    private var onAir = List[StreamOnAir]()

    def receive = {

      case Get => sender ! onAir

      case Search =>
        val keyword = "lichess.org"
        val max = 2
        val twitch = WS.url("https://api.twitch.tv/kraken/search/streams")
          .withQueryString("q" -> keyword)
          .withHeaders("Accept" -> "application/vnd.twitchtv.v2+json")
          .get().map { _.json.asOpt[Twitch.Result] ?? (_.streamsOnAir take max) }
        val ustream = WS.url(s"http://api.ustream.tv/json/channel/live/search/title:like:$keyword")
          .withQueryString("key" -> ustreamApiKey)
          .get().map { _.json.asOpt[Ustream.Result] ?? (_.streamsOnAir take max) }
        twitch |+| ustream map StreamsOnAir.apply pipeTo self

      case StreamsOnAir(streams) => onAir = streams
    }
  }))

  actor ! Search
}

object Streaming {

  private case object Get
  private[tv] case object Search
}
