package lila.streamer

import akka.actor._
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.Play.current
import scala.concurrent.duration._

import lila.db.dsl._
import lila.user.User

private final class Streaming(
    renderer: ActorSelection,
    api: StreamerApi,
    isOnline: User.ID => Boolean,
    timeline: ActorSelection,
    keyword: Stream.Keyword,
    alwaysFeatured: () => lila.common.Strings,
    googleApiKey: String,
    twitchClientId: String,
    lightUserApi: lila.user.LightUserApi
)
  extends Actor {

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
    streams = LiveStreams {
      scala.util.Random.shuffle {
        (twitchStreams ::: youTubeStreams) |> dedupStreamers
      }
    }
    _ <- api.setLiveNow(streamers.filter(streams.has).map(_.id))
  } yield publishStreams(streamers, streams)

  def publishStreams(streamers: List[Streamer], newStreams: LiveStreams) = {
    import makeTimeout.short
    import akka.pattern.ask
    if (newStreams != liveStreams) {
      renderer ? newStreams.autoFeatured.withTitles(lightUserApi) foreach {
        case html: String =>
          context.system.lilaBus.publish(lila.hub.actorApi.streamer.StreamsOnAir(html), 'streams)
      }
      newStreams.streams filterNot { s =>
        liveStreams has s.streamer
      } foreach { s =>
        timeline ! {
          import lila.hub.actorApi.timeline.{ Propagate, StreamStart }
          Propagate(StreamStart(s.streamer.userId, s.streamer.name.value)) toFollowersOf s.streamer.userId
        }
        context.system.lilaBus.publish(
          lila.hub.actorApi.streamer.StreamStart(s.streamer.userId),
          'streamStart
        )
      }
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

  def fetchTwitchStreams(streamers: List[Streamer]): Fu[List[Twitch.Stream]] = {
    val userIds = streamers.flatMap(_.twitch).map(_.userId.toLowerCase)
    userIds.nonEmpty ?? WS.url("https://api.twitch.tv/kraken/streams")
      .withQueryString(
        "channel" -> userIds.mkString(","),
        "stream_type" -> "live"
      )
      .withHeaders(
        "Accept" -> "application/vnd.twitchtv.v3+json",
        "Client-ID" -> twitchClientId
      )
      .get().map { res =>
        res.json.validate[Twitch.Result](twitchResultReads) match {
          case JsSuccess(data, _) => data.streams(
            keyword,
            streamers,
            alwaysFeatured().value.map(_.toLowerCase)
          )
          case JsError(err) =>
            logger.warn(s"twitch ${res.status} $err ${~res.body.lines.toList.headOption}")
            Nil
        }
      }
  }

  def fetchYouTubeStreams(streamers: List[Streamer]): Fu[List[YouTube.Stream]] = googleApiKey.nonEmpty ?? {
    val youtubeStreamers = streamers.filter(_.youTube.isDefined)
    youtubeStreamers.nonEmpty ?? WS.url("https://www.googleapis.com/youtube/v3/search").withQueryString(
      "part" -> "snippet",
      "type" -> "video",
      "eventType" -> "live",
      "q" -> keyword.value,
      "key" -> googleApiKey
    ).get().map { res =>
        res.json.validate[YouTube.Result](youtubeResultReads) match {
          case JsSuccess(data, _) => data.streams(keyword, youtubeStreamers)
          case JsError(err) =>
            logger.warn(s"youtube ${res.status} $err ${~res.body.lines.toList.headOption}")
            Nil
        }
      }
  }

  def dedupStreamers(streams: List[Stream]): List[Stream] = streams.foldLeft((Set.empty[Streamer.Id], List.empty[Stream])) {
    case ((streamerIds, streams), stream) if streamerIds(stream.streamer.id) => (streamerIds, streams)
    case ((streamerIds, streams), stream) => (streamerIds + stream.streamer.id, stream :: streams)
  }._2
}

object Streaming {

  case object Get
}
