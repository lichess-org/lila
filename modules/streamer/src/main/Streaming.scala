package lila.streamer

import akka.actor._
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.Play.current
import play.twirl.api.Html
import scala.concurrent.duration._

import lila.db.dsl._
import lila.user.User

private final class Streaming(
    renderer: ActorSelection,
    api: StreamerApi,
    isOnline: User.ID => Boolean,
    keyword: Stream.Keyword,
    googleApiKey: String,
    twitchClientId: String
) extends Actor {

  import Stream._
  import Twitch.Reads._
  import YouTube.Reads._
  import BsonHandlers._

  private case object Tick

  private var liveStreams = LiveStreams(Nil)

  def receive = {

    case Streaming.Get => sender ! liveStreams

    case Tick => updateStreams addEffectAnyway scheduleTick
  }

  private def scheduleTick = context.system.scheduler.scheduleOnce(15 seconds, self, Tick)

  self ! Tick

  def updateStreams: Funit = for {
    streamers <- api.allListed.map {
      _.filter { streamer =>
        liveStreams.has(streamer) || isOnline(streamer.userId)
      }
    }
    (twitchStreams, youTubeStreams) <- fetchTwitchStreams(streamers) zip fetchYouTubeStreams(streamers)
    streams = LiveStreams(twitchStreams ::: youTubeStreams)
    _ <- api.setLiveNow(streamers.filter(streams.has).map(_.id))
  } yield publishStreams(streamers, streams)

  def publishStreams(streamers: List[Streamer], newStreams: LiveStreams) = {
    import makeTimeout.short
    import akka.pattern.ask
    if (newStreams != liveStreams) renderer ? newStreams foreach {
      case html: play.twirl.api.Html =>
        context.system.lilaBus.publish(lila.hub.actorApi.StreamsOnAir(html.body), 'streams)
    }
    liveStreams = newStreams
    streamers foreach { streamer =>
      streamer.twitch.foreach { t =>
        lila.mon.tv.stream.name(s"${t.userId}@twitch") {
          if (liveStreams.streams.exists(s => s.serviceName == "twitch" && s.is(streamer))) 1 else 0
        }
      }
      streamer.youTube.foreach { t =>
        lila.mon.tv.stream.name(s"${t.channelId}@youtube") {
          if (liveStreams.streams.exists(s => s.serviceName == "youTube" && s.is(streamer))) 1 else 0
        }
      }
    }
  }

  def fetchTwitchStreams(streamers: List[Streamer]): Fu[List[Twitch.Stream]] =
    WS.url("https://api.twitch.tv/kraken/streams")
      .withQueryString("channel" -> streamers.flatMap(_.twitch).map(_.userId).mkString(","))
      .withHeaders(
        "Accept" -> "application/vnd.twitchtv.v3+json",
        "Client-ID" -> twitchClientId
      )
      .get() map { res =>
        res.json.validate[Twitch.Result](twitchResultReads) match {
          case JsSuccess(data, _) => data.streams(keyword, streamers)
          case JsError(err) =>
            logger.warn(s"twitch ${res.status} $err ${~res.body.lines.toList.headOption}")
            Nil
        }
      }

  def fetchYouTubeStreams(streamers: List[Streamer]): Fu[List[YouTube.Stream]] = googleApiKey.nonEmpty ?? {
    WS.url("https://www.googleapis.com/youtube/v3/search").withQueryString(
      "part" -> "snippet",
      "type" -> "video",
      "eventType" -> "live",
      "q" -> keyword.value,
      "key" -> googleApiKey
    ).get() map { res =>
        res.json.validate[YouTube.Result](youtubeResultReads) match {
          case JsSuccess(data, _) => data.streams(keyword, streamers)
          case JsError(err) =>
            logger.warn(s"youtube ${res.status} $err ${~res.body.lines.toList.headOption}")
            Nil
        }
      }
  }
}

object Streaming {

  case object Get
}
