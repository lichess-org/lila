package lila.tv

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.Play.current

private final class Streaming(
    system: ActorSystem,
    renderer: ActorSelection,
    streamerList: StreamerList) {

  import Streaming._
  import Twitch.Reads._
  import Hitbox.Reads._

  def onAir: Fu[List[StreamOnAir]] = {
    import makeTimeout.short
    actor ? Get mapTo manifest[List[StreamOnAir]]
    // fuccess(List(StreamOnAir(
    //   service = "twitch",
    //   name = "Chess master streams at lichess.org",
    //   streamer = "ChessNetwork",
    //   url = "http://foo.com",
    //   streamId = "test")))
  }

  private[tv] val actor = system.actorOf(Props(new Actor {

    private var onAir = List[StreamOnAir]()

    def receive = {

      case Get => sender ! onAir

      case Search => streamerList.get.map(_.filter(_.featured)).foreach { streamers =>
        val max = 5
        val keyword = "lichess.org"
        val twitch = WS.url("https://api.twitch.tv/kraken/streams")
          .withQueryString("channel" -> streamers.filter(_.twitch).map(_.streamerName).mkString(","))
          .withHeaders("Accept" -> "application/vnd.twitchtv.v3+json")
          .get() map { res =>
            res.json.validate[Twitch.Result] match {
              case JsSuccess(data, _) => data.streamsOnAir filter (_.name.toLowerCase contains keyword) take max
              case JsError(err) =>
                logwarn(s"twitch ${res.status} $err ${~res.body.lines.toList.headOption}")
                Nil
            }
          }
        val hitbox = WS.url("http://api.hitbox.tv/media/live/" + streamers.filter(_.twitch).map(_.streamerName).mkString(",")).get() map { res =>
          res.json.validate[Hitbox.Result] match {
            case JsSuccess(data, _) => data.streamsOnAir filter (_.name.toLowerCase contains keyword) take max
            case JsError(err) =>
              logwarn(s"hitbox ${res.status} $err ${~res.body.lines.toList.headOption}")
              Nil
          }
        }
        (twitch |+| hitbox) flatMap { streams =>
          streams.map { s =>
            whitelist withChat s.streamer map s.withChat
          }.sequenceFu
        } map StreamsOnAir.apply pipeTo self
      }

      case event@StreamsOnAir(streams) if onAir != streams =>
        onAir = streams
        import makeTimeout.short
        renderer ? event foreach {
          case html: play.twirl.api.Html =>
            context.system.lilaBus.publish(lila.hub.actorApi.StreamsOnAir(html.body), 'streams)
        }
    }
  }))

  actor ! Search
}

object Streaming {

  private case object Get
  private[tv] case object Search
}
