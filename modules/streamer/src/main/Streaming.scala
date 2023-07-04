package lila.streamer

import scala.util.chaining.*
import ornicar.scalalib.ThreadLocalRandom

import lila.common.{ Bus, LilaScheduler }

final private class Streaming(
    api: StreamerApi,
    isOnline: lila.socket.IsOnline,
    keyword: Stream.Keyword,
    alwaysFeatured: () => lila.common.UserIds,
    twitchApi: TwitchApi,
    ytApi: YouTubeApi
)(using Executor, Scheduler):

  import Stream.*

  private var liveStreams = LiveStreams(Nil)

  def getLiveStreams: LiveStreams = liveStreams

  LilaScheduler("Streaming", _.Every(15 seconds), _.AtMost(10 seconds), _.Delay(20 seconds)) {
    for
      streamerIds <- api.allListedIds
      activeIds = streamerIds.filter { id =>
        liveStreams.has(id) || isOnline(id.userId)
      }
      streamers <- api byIds activeIds
      (twitchStreams, youTubeStreams) <-
        twitchApi.fetchStreams(streamers, 0, None) map {
          _.collect { case Twitch.TwitchStream(name, title, _, language) =>
            streamers.find { s =>
              s.twitch.exists(_.userId.toLowerCase == name.toLowerCase) && {
                title.value.toLowerCase.contains(keyword.toLowerCase) ||
                alwaysFeatured().value.contains(s.userId)
              }
            } map { Twitch.Stream(name, title, _, language) }
          }.flatten
        } zip ytApi.fetchStreams(streamers)
      streams = LiveStreams {
        ThreadLocalRandom.shuffle {
          (twitchStreams ::: youTubeStreams) pipe dedupStreamers
        }
      }
      _ <- api.setLangLiveNow(streams.streams)
    yield publishStreams(streamers, streams)
  }

  private val streamStartOnceEvery = lila.memo.OnceEvery[UserId](2 hour)

  private def publishStreams(streamers: List[Streamer], newStreams: LiveStreams) =
    if newStreams != liveStreams then
      newStreams.streams filterNot { s =>
        liveStreams has s.streamer
      } foreach { s =>
        import s.streamer.userId
        if streamStartOnceEvery(userId) then
          Bus.publish(
            lila.hub.actorApi.streamer.StreamStart(userId, s.streamer.name.value),
            "streamStart"
          )
      }
    liveStreams = newStreams
    streamers foreach { streamer =>
      streamer.twitch.foreach { t =>
        if liveStreams.streams.exists(s => s.serviceName == "twitch" && s.is(streamer)) then
          lila.mon.tv.streamer.present(s"${t.userId}@twitch").increment()
      }
      streamer.youTube.foreach { t =>
        if liveStreams.streams.exists(s => s.serviceName == "youTube" && s.is(streamer)) then
          lila.mon.tv.streamer.present(s"${t.channelId}@youtube").increment()
      }
    }

  private def dedupStreamers(streams: List[Stream]): List[Stream] =
    streams
      .foldLeft((Set.empty[Streamer.Id], List.empty[Stream])) {
        case ((streamerIds, streams), stream) if streamerIds(stream.streamer.id) => (streamerIds, streams)
        case ((streamerIds, streams), stream) => (streamerIds + stream.streamer.id, stream :: streams)
      }
      ._2
