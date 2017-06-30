package lila.tv

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.Play.current

private final class Streaming(
    system: ActorSystem,
    renderer: ActorSelection,
    streamerList: StreamerList,
    keyword: String,
    googleApiKey: String,
    twitchClientId: String
) {

  import Streaming._
  import Twitch.Reads._
  import Youtube.Reads._

  def onAir: Fu[List[StreamOnAir]] = {
    import makeTimeout.short
    actor ? Get mapTo manifest[List[StreamOnAir]]
    // fuccess(List(StreamOnAir(
    //   name = "Chess master streams at lichess.org",
    //   streamer = StreamerList.Streamer(
    //     service = StreamerList.Youtube,
    //     streamerName = "UCVeETS7uZTAARqvv2zssZCw",
    //     lichessName = "test",
    //     streamerNameForDisplay = "test".some,
    //     featured = true,
    //     chat = true
    //   ),
    //   url = "http://foo.com",
    //   streamId = "UCVeETS7uZTAARqvv2zssZCw"
    // ), StreamOnAir(
    //   name = "[fr] some french stream",
    //   streamer = StreamerList.Streamer(
    //     service = StreamerList.Twitch,
    //     streamerName = "fr_guy",
    //     streamerNameForDisplay = "fr guy".some,
    //     lichessName = "fr_guy",
    //     featured = true,
    //     chat = true
    //   ),
    //   url = "http://foo.com",
    //   streamId = "test_fr"
    // ), StreamOnAir(
    //   name = "[ES] some spanish stream",
    //   streamer = StreamerList.Streamer(
    //     service = StreamerList.Twitch,
    //     streamerName = "es_guy",
    //     streamerNameForDisplay = "es guy".some,
    //     lichessName = "es_guy",
    //     featured = true,
    //     chat = true
    //   ),
    //   url = "http://foo.com",
    //   streamId = "test_es"
    // )))
  }

  private[tv] val actor = system.actorOf(Props(new Actor {

    private var onAir = List[StreamOnAir]()

    def receive = {

      case Get => sender ! onAir

      case Search => streamerList.get.map(_.filter(_.featured)).foreach { streamers =>
        val max = 5
        val twitch = WS.url("https://api.twitch.tv/kraken/streams")
          .withQueryString("channel" -> streamers.filter(_.twitch).map(_.streamerName).mkString(","))
          .withHeaders(
            "Accept" -> "application/vnd.twitchtv.v3+json",
            "Client-ID" -> twitchClientId
          )
          .get() map { res =>
            res.json.validate[Twitch.Result] match {
              case JsSuccess(data, _) => data.streamsOnAir(streamers) filter (_.name.toLowerCase contains keyword) take max
              case JsError(err) =>
                logger.warn(s"twitch ${res.status} $err ${~res.body.lines.toList.headOption}")
                Nil
            }
          }
        val youtube = googleApiKey.nonEmpty ?? {
          WS.url("https://www.googleapis.com/youtube/v3/search").withQueryString(
            "part" -> "snippet",
            "type" -> "video",
            "eventType" -> "live",
            "q" -> keyword,
            "key" -> googleApiKey
          ).get() map { res =>
              res.json.validate[Youtube.Result] match {
                case JsSuccess(data, _) => data.streamsOnAir(streamers) filter (_.name.toLowerCase contains keyword) take max
                case JsError(err) =>
                  logger.warn(s"youtube ${res.status} $err ${~res.body.lines.toList.headOption}")
                  Nil
              }
            }
        }
        (twitch |+| youtube) map { ss =>
          StreamsOnAir {
            ss.foldLeft(List.empty[StreamOnAir]) {
              case (acc, s) if acc.exists(_.id == s.id) => acc
              case (acc, s) => acc :+ s
            }
          }
        } pipeTo self
      }

      case event @ StreamsOnAir(streams) =>
        if (onAir != streams) {
          onAir = streams
          import makeTimeout.short
          renderer ? event foreach {
            case html: play.twirl.api.Html =>
              context.system.lilaBus.publish(lila.hub.actorApi.StreamsOnAir(html.body), 'streams)
          }
        }
        streamerList.get foreach { all =>
          all foreach { streamer =>
            lila.mon.tv.stream.name(streamer.id) {
              if (streams.exists(_ is streamer)) 1 else 0
            }
          }
        }
    }
  }))

  actor ! Search
}

object Streaming {

  private case object Get
  private[tv] case object Search
}
