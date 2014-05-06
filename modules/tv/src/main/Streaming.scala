package lila.tv

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.ws.WS

private final class Streaming(
    system: ActorSystem,
    ustreamApiKey: String,
    renderer: ActorSelection,
    isOnline: String => Boolean) {

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
        val max = 2
        val twitch = WS.url("https://api.twitch.tv/kraken/search/streams")
          .withQueryString("q" -> "lichess.org")
          .withHeaders("Accept" -> "application/vnd.twitchtv.v2+json")
          .get().map { res =>
            res.json.asOpt[Twitch.Result] match {
              case Some(data) => data.streamsOnAir take max
              case None =>
                logger.warn(s"twitch ${res.status} ${~res.body.lines.toList.headOption}")
                Nil
            }
          }
        val chesswhiz = isOnline("chesswhiz") ??
          WS.url(s"http://api.ustream.tv/json/stream/recent/search/title:like:ChessWhiz")
          .withQueryString("key" -> ustreamApiKey)
          .get().map { res =>
            res.json.asOpt[Ustream.Result] match {
              case Some(data) => data.streamsOnAir take max
              case None =>
                logger.warn(s"chesswhiz ${res.status} ${~res.body.lines.toList.headOption}")
                Nil
            }
          }
        twitch |+| chesswhiz map StreamsOnAir.apply pipeTo self

      case event@StreamsOnAir(streams) if onAir != streams =>
        onAir = streams
        import makeTimeout.short
        renderer ? event foreach {
          case html: play.api.templates.Html =>
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
