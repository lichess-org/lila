package lila.streamer

import akka.actor._
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.ws.WSClient
import scala.concurrent.duration._
import scala.util.chaining._

import lila.common.Bus
import lila.common.config.Secret
import lila.user.User

final private class Streaming(
    ws: WSClient,
    api: StreamerApi,
    isOnline: User.ID => Boolean,
    timeline: lila.hub.actors.Timeline,
    keyword: Stream.Keyword,
    alwaysFeatured: () => lila.common.Strings,
    googleApiKey: Secret,
    twitchCredentials: () => (String, String)
) extends Actor {

  import Stream._
  import Twitch.Reads._
  import YouTube.Reads._

  private case object Tick

  private var liveStreams = LiveStreams(Nil)

  implicit def ec = context.dispatcher

  def receive = {

    case Streaming.Get => sender() ! liveStreams

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
      streamers <- api byIds activeIds
      (twitchStreams, youTubeStreams) <-
        fetchTwitchStreams(streamers, 0, None, Nil) zip fetchYouTubeStreams(streamers)
      streams = LiveStreams {
        scala.util.Random.shuffle {
          (twitchStreams ::: youTubeStreams) pipe dedupStreamers
        }
      }
      _ <- api.setLiveNow(streamers.filter(streams.has).map(_.id))
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

  def fetchTwitchStreams(
      streamers: List[Streamer],
      page: Int,
      pagination: Option[Twitch.Pagination],
      acc: List[Twitch.Stream]
  ): Fu[List[Twitch.Stream]] = {
    val (clientId, secret) = twitchCredentials()
    if (clientId.nonEmpty && secret.nonEmpty && page < 10) {
      val query = List(
        "game_id" -> "368914", // shogi
        "first"   -> "100" // max results per page
      ) ::: List(
        pagination.flatMap(_.cursor).map { "after" -> _ }
      ).flatten
      ws.url("https://api.twitch.tv/helix/streams")
        .withQueryStringParameters(query: _*)
        .withHttpHeaders(
          "Client-ID"     -> clientId,
          "Authorization" -> s"Bearer $secret"
        )
        .get()
        .flatMap {
          case res if res.status == 200 =>
            res.json.validate[Twitch.Result](twitchResultReads) match {
              case JsSuccess(result, _) => fuccess(result)
              case JsError(err)         => fufail(s"twitch $err ${lila.log http res}")
            }
          case res => fufail(s"twitch ${lila.log http res}")
        }
        .recover {
          case e: Exception =>
            logger.warn(e.getMessage)
            Twitch.Result(None, None)
        }
        .monSuccess(_.tv.streamer.twitch)
        .flatMap { result =>
          if (result.data.exists(_.nonEmpty))
            fetchTwitchStreams(
              streamers,
              page + 1,
              result.pagination,
              acc ::: result.streams(
                keyword,
                streamers,
                alwaysFeatured().value.map(_.toLowerCase)
              )
            )
          else fuccess(acc)
        }
    } else fuccess(acc)
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
              res.json.validate[YouTube.Result](youtubeResultReads) match {
                case JsSuccess(data, _) =>
                  fuccess(YouTube.StreamsFetched(data.streams(keyword, youtubeStreamers), now))
                case JsError(err) =>
                  fufail(s"youtube ${res.status} $err ${res.body.take(500)}")
              }
            }
            .monSuccess(_.tv.streamer.youTube)
            .recover {
              case e: Exception =>
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
