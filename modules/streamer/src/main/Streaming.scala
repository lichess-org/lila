package lila.streamer

import akka.actor._
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.ws.WSClient
import scala.concurrent.duration._

import lila.common.Bus
import lila.common.config.Secret
import lila.user.User

final private class Streaming(
    ws: WSClient,
    renderer: lila.hub.actors.Renderer,
    api: StreamerApi,
    isOnline: User.ID => Boolean,
    timeline: lila.hub.actors.Timeline,
    keyword: Stream.Keyword,
    alwaysFeatured: () => lila.common.Strings,
    googleApiKey: Secret,
    twitchClientId: Secret,
    lightUserApi: lila.user.LightUserApi
) extends Actor {

  import Stream._
  import Twitch.Reads._
  import YouTube.Reads._

  private case object Tick

  private var liveStreams = LiveStreams(Nil)

  implicit def ec = context.dispatcher

  def receive = {

    case Streaming.Get => sender ! liveStreams

    case Tick => updateStreams addEffectAnyway scheduleTick
  }

  private def scheduleTick = context.system.scheduler.scheduleOnce(15 seconds, self, Tick)

  self ! Tick

  def updateStreams: Funit =
    for {
      streamerIds <- api.allListedIds
      activeIds = streamerIds.filter { id =>
        liveStreams.has(id) || isOnline(id.value)
      }
      streamers                       <- api byIds activeIds
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
      renderer.actor ? newStreams.autoFeatured.withTitles(lightUserApi) foreach {
        case html: String =>
          Bus.publish(lila.hub.actorApi.streamer.StreamsOnAir(html), "streams")
      }
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

  def fetchTwitchStreams(streamers: List[Streamer]): Fu[List[Twitch.Stream]] = {
    val maxIds = 100
    val allTwitchStreamers = streamers.flatMap { s =>
      s.twitch map (s.id -> _)
    }
    val futureTwitchStreamers: Fu[List[Streamer.Twitch]] =
      if (allTwitchStreamers.size > maxIds)
        api.mostRecentlySeenIds(allTwitchStreamers.map(_._1), maxIds) map { ids =>
          allTwitchStreamers collect {
            case (streamerId, twitch) if ids(streamerId) => twitch
          }
        } else fuccess(allTwitchStreamers.map(_._2))
    futureTwitchStreamers flatMap { twitchStreamers =>
      twitchStreamers.nonEmpty ?? {
        val twitchUserIds = twitchStreamers.map(_.userId)
        val url = ws
          .url("https://api.twitch.tv/helix/streams")
          .withQueryStringParameters(
            (("first" -> maxIds.toString) :: twitchUserIds.map("user_login" -> _)): _*
          )
          .withHttpHeaders(
            "Client-ID" -> twitchClientId.value
          )
        url
          .get()
          .map { res =>
            res.json.validate[Twitch.Result](twitchResultReads) match {
              case JsSuccess(data, _) =>
                data.streams(
                  keyword,
                  streamers,
                  alwaysFeatured().value.map(_.toLowerCase)
                )
              case JsError(err) =>
                logger.warn(s"twitch ${res.status} $err ${~res.body.linesIterator.toList.headOption}")
                Nil
            }
          }
          .monSuccess(_.tv.streamer.twitch)
      }
    }
  }

  private var prevYouTubeStreams = YouTube.StreamsFetched(Nil, DateTime.now)

  def fetchYouTubeStreams(streamers: List[Streamer]): Fu[List[YouTube.Stream]] = {
    val youtubeStreamers = streamers.filter(_.youTube.isDefined)
    (youtubeStreamers.nonEmpty && googleApiKey.value.nonEmpty) ?? {
      val now = DateTime.now
      val res =
        if (prevYouTubeStreams.list.isEmpty && prevYouTubeStreams.at.isAfter(now minusMinutes 2))
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
            .map { res =>
              res.json.validate[YouTube.Result](youtubeResultReads) match {
                case JsSuccess(data, _) =>
                  YouTube.StreamsFetched(data.streams(keyword, youtubeStreamers), now)
                case JsError(err) =>
                  logger.warn(s"youtube ${res.status} $err ${res.body.take(500)}")
                  YouTube.StreamsFetched(Nil, now)
              }
            }
            .monSuccess(_.tv.streamer.youTube)
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
