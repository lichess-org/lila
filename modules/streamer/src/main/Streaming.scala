package lila.streamer

import akka.actor._
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.StandaloneWSClient
import scala.concurrent.duration._
import scala.util.chaining._

import lila.common.Bus
import lila.common.config.Secret
import lila.user.User

final private class Streaming(
    ws: StandaloneWSClient,
    api: StreamerApi,
    isOnline: User.ID => Boolean,
    timeline: lila.hub.actors.Timeline,
    keyword: Stream.Keyword,
    alwaysFeatured: () => lila.common.UserIds,
    googleApiKey: Secret,
    twitchApi: TwitchApi
) extends Actor {

  import Stream._
  import YouTube.Reads._

  private case object Tick

  private var liveStreams = LiveStreams(Nil)

  implicit def ec = context.dispatcher

  def receive = {

    case Streaming.Get => sender() ! liveStreams

    case Tick => updateStreams.addEffectAnyway(scheduleTick()).unit
  }

  private def scheduleTick(): Unit = context.system.scheduler.scheduleOnce(15 seconds, self, Tick).unit

  scheduleTick()

  def updateStreams: Funit =
    for {
      streamerIds <- api.allListedIds
      activeIds = streamerIds.filter { id =>
        liveStreams.has(id) || isOnline(id.value)
      }
      streamers <- api byIds activeIds
      (twitchStreams, youTubeStreams) <-
        twitchApi.fetchStreams(streamers, 0, None) map {
          _.collect { case Twitch.TwitchStream(name, title, _) =>
            streamers.find { s =>
              s.twitch.exists(_.userId.toLowerCase == name.toLowerCase) && {
                title.toLowerCase.contains(keyword.toLowerCase) ||
                alwaysFeatured().value.contains(s.userId)
              }
            } map { Twitch.Stream(name, title, _) }
          }.flatten
        } zip fetchYouTubeStreams(streamers)
      streams = LiveStreams {
        lila.common.ThreadLocalRandom.shuffle {
          (twitchStreams ::: youTubeStreams) pipe dedupStreamers
        }
      }
      _ <- api.setLiveNow(streamers.withFilter(streams.has).map(_.id))
    } yield publishStreams(streamers, streams)

  def publishStreams(streamers: List[Streamer], newStreams: LiveStreams) = {
    if (newStreams != liveStreams) {
      newStreams.streams filterNot { s =>
        liveStreams has s.streamer
      } foreach { s =>
        timeline ! {
          import lila.hub.actorApi.timeline.{ Propagate, StreamStart }
          Propagate(StreamStart(s.streamer.userId, s.streamer.name.value)) toFollowersOf s.streamer.userId
        }
        Bus.publish(
          lila.hub.actorApi.streamer.StreamStart(s.streamer.userId),
          "streamStart"
        )
      }
    }
    liveStreams = newStreams
    streamers foreach { streamer =>
      streamer.twitch.foreach { t =>
        if (liveStreams.streams.exists(s => s.serviceName == "twitch" && s.is(streamer)))
          lila.mon.tv.streamer.present(s"${t.userId}@twitch").increment()
      }
      streamer.youTube.foreach { t =>
        if (liveStreams.streams.exists(s => s.serviceName == "youTube" && s.is(streamer)))
          lila.mon.tv.streamer.present(s"${t.channelId}@youtube").increment()
      }
    }
  }

  private var prevYouTubeStreams = YouTube.StreamsFetched(Nil, DateTime.now)

  def fetchYouTubeStreams(streamers: List[Streamer]): Fu[List[YouTube.Stream]] = {
    val youtubeStreamers = streamers.filter(_.youTube.isDefined)
    (youtubeStreamers.nonEmpty && googleApiKey.value.nonEmpty) ?? {
      val now = DateTime.now
      val res =
        if (prevYouTubeStreams.list.isEmpty && prevYouTubeStreams.at.isAfter(now minusMinutes 3))
          fuccess(prevYouTubeStreams)
        else if (prevYouTubeStreams.at.isAfter(now minusMinutes 1))
          fuccess(prevYouTubeStreams)
        else {
          ws.url("https://www.googleapis.com/youtube/v3/search")
            .withQueryStringParameters(
              "part"      -> "snippet",
              "type"      -> "video",
              "eventType" -> "live",
              "q"         -> keyword.value,
              "key"       -> googleApiKey.value
            )
            .get()
            .flatMap { res =>
              res.body[JsValue].validate[YouTube.Result](youtubeResultReads) match {
                case JsSuccess(data, _) =>
                  fuccess(YouTube.StreamsFetched(data.streams(keyword, youtubeStreamers), now))
                case JsError(err) =>
                  fufail(s"youtube ${res.status} $err ${res.body.take(500)}")
              }
            }
            .monSuccess(_.tv.streamer.youTube)
            .recover { case e: Exception =>
              logger.warn(e.getMessage)
              YouTube.StreamsFetched(Nil, now)
            }
        }
      res dmap { r =>
        prevYouTubeStreams = r
        r.list
      }
    }
  }

  def dedupStreamers(streams: List[Stream]): List[Stream] =
    streams
      .foldLeft((Set.empty[Streamer.Id], List.empty[Stream])) {
        case ((streamerIds, streams), stream) if streamerIds(stream.streamer.id) => (streamerIds, streams)
        case ((streamerIds, streams), stream)                                    => (streamerIds + stream.streamer.id, stream :: streams)
      }
      ._2
}

object Streaming {

  case object Get
}
