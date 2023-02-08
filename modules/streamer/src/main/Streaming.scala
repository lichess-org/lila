package lila.streamer

import org.joda.time.DateTime
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.StandaloneWSClient
import scala.concurrent.duration.*
import scala.util.chaining.*
import ornicar.scalalib.ThreadLocalRandom

import lila.common.{ Bus, LilaScheduler }
import lila.common.config.Secret
import lila.user.User

final private class Streaming(
    ws: StandaloneWSClient,
    api: StreamerApi,
    isOnline: lila.socket.IsOnline,
    keyword: Stream.Keyword,
    alwaysFeatured: () => lila.common.UserIds,
    googleApiKey: Secret,
    twitchApi: TwitchApi
)(using
    ec: scala.concurrent.ExecutionContext,
    scheduler: akka.actor.Scheduler
):

  import Stream.*
  import YouTube.given

  private var liveStreams = LiveStreams(Nil)

  def getLiveStreams: LiveStreams = liveStreams

  LilaScheduler("Streaming", _.Every(15 seconds), _.AtMost(10 seconds), _.Delay(20 seconds)) {
    for {
      streamerIds <- api.allListedIds
      activeIds = streamerIds.filter { id =>
        liveStreams.has(id) || isOnline.value(id.userId)
      }
      streamers <- api byIds activeIds
      (twitchStreams, youTubeStreams) <-
        twitchApi.fetchStreams(streamers, 0, None) map {
          _.collect { case Twitch.TwitchStream(name, title, _, language) =>
            streamers.find { s =>
              s.twitch.exists(_.userId.toLowerCase == name.toLowerCase) && {
                title.toLowerCase.contains(keyword.toLowerCase) ||
                alwaysFeatured().value.contains(s.userId)
              }
            } map { Twitch.Stream(name, title, _, language) }
          }.flatten
        } zip fetchYouTubeStreams(streamers)
      streams = LiveStreams {
        ThreadLocalRandom.shuffle {
          (twitchStreams ::: youTubeStreams) pipe dedupStreamers
        }
      }
      _ <- api.setLangLiveNow(streams.streams)
    } yield publishStreams(streamers, streams)
  }

  private val streamStartOnceEvery = lila.memo.OnceEvery[UserId](2 hour)

  private def publishStreams(streamers: List[Streamer], newStreams: LiveStreams) =
    if (newStreams != liveStreams)
      newStreams.streams filterNot { s =>
        liveStreams has s.streamer
      } foreach { s =>
        import s.streamer.userId
        if (streamStartOnceEvery(userId))
          Bus.publish(
            lila.hub.actorApi.streamer.StreamStart(userId, s.streamer.name.value),
            "notify"
          )
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

  private var prevYouTubeStreams = YouTube.StreamsFetched(Nil, DateTime.now)

  private def fetchYouTubeStreams(streamers: List[Streamer]): Fu[List[YouTube.Stream]] =
    val youtubeStreamers = streamers.filter(_.youTube.isDefined)
    (youtubeStreamers.nonEmpty && googleApiKey.value.nonEmpty) ?? {
      val now = DateTime.now
      val res =
        if (prevYouTubeStreams.at.isAfter(now minusMinutes 15))
          fuccess(prevYouTubeStreams)
        else
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
              res.body[JsValue].validate[YouTube.Result] match
                case JsSuccess(data, _) =>
                  fuccess(YouTube.StreamsFetched(data.streams(keyword, youtubeStreamers), now))
                case JsError(err) =>
                  fufail(s"youtube ${res.status} $err ${res.body[String].take(200)}")
            }
            .monSuccess(_.tv.streamer.youTube)
            .recover { case e: Exception =>
              logger.warn(e.getMessage)
              YouTube.StreamsFetched(Nil, now)
            }
      res dmap { r =>
        logger.info(s"Fetched ${r.list.size} youtube streamers.")
        prevYouTubeStreams = r
        r.list
      }
    }

  private def dedupStreamers(streams: List[Stream]): List[Stream] =
    streams
      .foldLeft((Set.empty[Streamer.Id], List.empty[Stream])) {
        case ((streamerIds, streams), stream) if streamerIds(stream.streamer.id) => (streamerIds, streams)
        case ((streamerIds, streams), stream) => (streamerIds + stream.streamer.id, stream :: streams)
      }
      ._2
