package lila.tv

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.ws.WS
import play.api.Play.current

private final class Streaming(
    system: ActorSystem,
    ustreamApiKey: String,
    renderer: ActorSelection,
    whitelist: Whitelist) {

  import Streaming._
  import Twitch.Reads._
  // import Ustream.Reads._

  def onAir: Fu[List[StreamOnAir]] = {
    import makeTimeout.short
    actor ? Get mapTo manifest[List[StreamOnAir]]
  }

  private[tv] val actor = system.actorOf(Props(new Actor {

    private var onAir = List[StreamOnAir]()

    def receive = {

      case Get => sender ! onAir

      case Search =>
        val max = 3
        val keyword = "lichess.org"
        val twitch = whitelist.apply zip WS.url("https://api.twitch.tv/kraken/search/streams")
          .withQueryString("q" -> keyword)
          .withHeaders("Accept" -> "application/vnd.twitchtv.v2+json")
          .get() map {
            case (authorizedStreamers, res) =>
              res.json.asOpt[Twitch.Result] match {
                case Some(data) => data.streamsOnAir filter { stream =>
                  authorizedStreamers contains stream.streamer.toLowerCase
                } filter { stream =>
                  stream.name contains keyword
                } take max
                case None =>
                  logger.warn(s"twitch ${res.status} ${~res.body.lines.toList.headOption}")
                  Nil
              }
          }
        twitch map StreamsOnAir.apply pipeTo self

      case event@StreamsOnAir(streams) if onAir != streams =>
        onAir = streams
        import makeTimeout.short
        renderer ? event foreach {
          case html: play.twirl.api.Html =>
            context.system.lilaBus.publish(lila.hub.actorApi.StreamsOnAir(html.body), 'streams)
        }
    }
  }))

  private def logger = play.api.Logger("tv.streaming")

  actor ! Search
}

object Streaming {

  private case object Get
  private[tv] case object Search
}
